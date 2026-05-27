package com.eprocure.approval.application.usecase;

import com.eprocure.approval.application.port.in.GetApprovalInboxQuery;
import com.eprocure.approval.application.port.out.UserResolverPort;
import com.eprocure.approval.application.service.ApprovalTaskSummary;
import com.eprocure.approval.application.service.PageMeta;
import com.eprocure.approval.application.service.PageResult;
import com.eprocure.approval.domain.repository.ApprovalProcessRepository;
import com.eprocure.approval.domain.repository.PendingTaskProjection;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetApprovalInboxUseCase {
    private static final Logger log = LogManager.getLogger(GetApprovalInboxUseCase.class);

    private final ApprovalProcessRepository approvalProcessRepository;
    private final UserResolverPort userResolverPort;

    public GetApprovalInboxUseCase(
            ApprovalProcessRepository approvalProcessRepository,
            UserResolverPort userResolverPort) {
        this.approvalProcessRepository = approvalProcessRepository;
        this.userResolverPort = userResolverPort;
    }

    @Transactional(readOnly = true)
    public PageResult<ApprovalTaskSummary> execute(GetApprovalInboxQuery query) {
        log.info("[ACTION] Start GetApprovalInbox | approverId={}", query.approverId());

        String orderByColumn = "slaDeadline";
        String sortDirection = "asc";
        if (query.sort() != null && !query.sort().isBlank()) {
            String[] parts = query.sort().split(",");
            if (parts.length > 0) {
                String col = parts[0].trim();
                if (col.equals("slaDeadline") || col.equals("assignedAt") || col.equals("totalAmount")) {
                    orderByColumn = col;
                }
            }
            if (parts.length > 1) {
                String dir = parts[1].trim().toLowerCase();
                if (dir.equals("asc") || dir.equals("desc")) {
                    sortDirection = dir;
                }
            }
        }

        BigDecimal minAmountVal = null;
        if (query.minAmountOpt().isPresent()) {
            try {
                minAmountVal = new BigDecimal(query.minAmountOpt().get());
            } catch (NumberFormatException exception) {
                log.warn("[ACTION] Step GetApprovalInbox | minAmount invalid format: {}", query.minAmountOpt().get());
            }
        }

        List<PendingTaskProjection> projections = approvalProcessRepository.findPendingTasks(
                query.approverId(),
                query.priorityOpt().orElse(null),
                query.entityTypeOpt().orElse(null),
                minAmountVal,
                query.isOverdueOpt().orElse(null),
                orderByColumn,
                sortDirection,
                query.offset(),
                query.size()
        );

        long total = approvalProcessRepository.countPendingTasks(
                query.approverId(),
                query.priorityOpt().orElse(null),
                query.entityTypeOpt().orElse(null),
                minAmountVal,
                query.isOverdueOpt().orElse(null)
        );

        Map<UUID, UserResolverPort.UserSummary> userCache = new HashMap<>();
        List<ApprovalTaskSummary> items = projections.stream().map(p -> {
            UserResolverPort.UserSummary requesterInfo = userCache.computeIfAbsent(p.requesterId(), id ->
                    userResolverPort.getUserById(id).orElse(null)
            );
            ApprovalTaskSummary.Requester requester;
            if (requesterInfo != null) {
                requester = new ApprovalTaskSummary.Requester(p.requesterId(), requesterInfo.fullName(), requesterInfo.departmentName());
            } else {
                requester = new ApprovalTaskSummary.Requester(p.requesterId(), "User (" + p.requesterId() + ")", "N/A");
            }

            boolean isDelegated = p.delegateId() != null && query.approverId().equals(p.delegateId());
            ApprovalTaskSummary.DelegatedFrom delegatedFrom = null;
            if (isDelegated) {
                UserResolverPort.UserSummary originalApproverInfo = userCache.computeIfAbsent(p.approverId(), id ->
                        userResolverPort.getUserById(id).orElse(null)
                );
                if (originalApproverInfo != null) {
                    delegatedFrom = new ApprovalTaskSummary.DelegatedFrom(p.approverId(), originalApproverInfo.fullName());
                } else {
                    delegatedFrom = new ApprovalTaskSummary.DelegatedFrom(p.approverId(), "User (" + p.approverId() + ")");
                }
            }

            com.eprocure.approval.application.service.SlaStatus sla = p.slaDeadline() != null ?
                    com.eprocure.approval.application.service.SlaStatus.calculate(p.slaDeadline(), java.time.Instant.now()) :
                    null;

            return new ApprovalTaskSummary(
                    p.camundaTaskId(),
                    p.processId(),
                    p.entityType(),
                    p.entityId(),
                    p.entityNumber(),
                    p.entityTitle(),
                    requester,
                    p.totalAmount(),
                    p.currency(),
                    p.priority(),
                    p.stepIndex(),
                    p.stepType(),
                    sla,
                    isDelegated,
                    delegatedFrom,
                    p.assignedAt()
            );
        }).toList();

        PageMeta meta = PageMeta.of(total, query.page(), query.size());
        PageResult<ApprovalTaskSummary> result = new PageResult<>(items, meta);

        log.info("[ACTION] Complete GetApprovalInbox | approverId={} | count={}", query.approverId(), items.size());
        return result;
    }
}
