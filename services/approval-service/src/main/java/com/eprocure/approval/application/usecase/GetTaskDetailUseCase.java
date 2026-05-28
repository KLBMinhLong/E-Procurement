package com.eprocure.approval.application.usecase;

import com.eprocure.approval.application.port.out.UserResolverPort;
import com.eprocure.approval.application.service.ApprovalProcessDetail;
import com.eprocure.approval.application.service.ApprovalTaskDetail;
import com.eprocure.approval.application.service.ApprovalTaskSummary;
import com.eprocure.approval.application.service.SlaStatus;
import com.eprocure.approval.common.exception.BusinessException;
import com.eprocure.approval.common.exception.ErrorCode;
import com.eprocure.approval.domain.model.ApprovalProcess;
import com.eprocure.approval.domain.model.ApprovalStep;
import com.eprocure.approval.domain.repository.ApprovalProcessRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetTaskDetailUseCase {
    private static final Logger log = LogManager.getLogger(GetTaskDetailUseCase.class);

    private final ApprovalProcessRepository approvalProcessRepository;
    private final UserResolverPort userResolverPort;

    public GetTaskDetailUseCase(
            ApprovalProcessRepository approvalProcessRepository,
            UserResolverPort userResolverPort) {
        this.approvalProcessRepository = approvalProcessRepository;
        this.userResolverPort = userResolverPort;
    }

    @Transactional(readOnly = true)
    public ApprovalTaskDetail execute(String taskId, UUID actorId) {
        log.info("[ACTION] Start GetTaskDetail | taskId={} | actorId={}", taskId, actorId);

        ApprovalProcess process = findRunningProcess(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APR_004));

        UUID stepId = resolveStepId(process, taskId);
        ApprovalStep activeStep = process.findStep(stepId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APR_004));

        if (!activeStep.isAssignedTo(actorId)) {
            throw new BusinessException(ErrorCode.APR_002);
        }

        // Cache for user resolution
        Map<UUID, UserResolverPort.UserSummary> userCache = new HashMap<>();

        // Resolve Requester
        UserResolverPort.UserSummary requesterInfo = userCache.computeIfAbsent(process.getRequesterId(), id ->
                userResolverPort.getUserById(id).orElse(null)
        );
        ApprovalTaskSummary.Requester requester;
        if (requesterInfo != null) {
            requester = new ApprovalTaskSummary.Requester(process.getRequesterId(), requesterInfo.fullName(), requesterInfo.departmentName());
        } else {
            requester = new ApprovalTaskSummary.Requester(process.getRequesterId(), "User (" + process.getRequesterId() + ")", "N/A");
        }

        // Resolve Delegate/Approver
        boolean isDelegated = activeStep.getDelegateId().isPresent() && activeStep.isAssignedTo(actorId);
        ApprovalTaskSummary.DelegatedFrom delegatedFrom = null;
        if (isDelegated) {
            UserResolverPort.UserSummary originalApproverInfo = userCache.computeIfAbsent(activeStep.getApproverId(), id ->
                    userResolverPort.getUserById(id).orElse(null)
            );
            if (originalApproverInfo != null) {
                delegatedFrom = new ApprovalTaskSummary.DelegatedFrom(activeStep.getApproverId(), originalApproverInfo.fullName());
            } else {
                delegatedFrom = new ApprovalTaskSummary.DelegatedFrom(activeStep.getApproverId(), "User (" + activeStep.getApproverId() + ")");
            }
        }

        SlaStatus sla = activeStep.getSlaDeadline() != null ?
                SlaStatus.calculate(activeStep.getSlaDeadline(), Instant.now()) :
                null;

        // 1. Build task summary
        ApprovalTaskSummary taskSummary = new ApprovalTaskSummary(
                activeStep.getCamundaTaskId().orElse(activeStep.getId().toString()),
                process.getId(),
                process.getEntityType().name(),
                process.getEntityId(),
                process.getEntityNumber(),
                process.getEntityTitle(),
                requester,
                process.getTotalAmount().amount(),
                process.getTotalAmount().currency(),
                process.getPriority().name(),
                activeStep.getStepIndex(),
                activeStep.getApproverRole(),
                sla,
                isDelegated,
                delegatedFrom,
                activeStep.getAssignedAt()
        );

        // 2. Build process steps history/trail
        List<ApprovalProcessDetail.StepDetail> stepsDetailList = new ArrayList<>();
        for (ApprovalStep step : process.getSteps()) {
            UserResolverPort.UserSummary appInfo = userCache.computeIfAbsent(step.getApproverId(), id ->
                    userResolverPort.getUserById(id).orElse(null)
            );
            ApprovalProcessDetail.Approver stepApprover;
            if (appInfo != null) {
                stepApprover = new ApprovalProcessDetail.Approver(step.getApproverId(), appInfo.fullName());
            } else {
                stepApprover = new ApprovalProcessDetail.Approver(step.getApproverId(), "User (" + step.getApproverId() + ")");
            }

            stepsDetailList.add(new ApprovalProcessDetail.StepDetail(
                    step.getStepIndex(),
                    step.getApproverRole(),
                    stepApprover,
                    step.getDelegateId().orElse(null),
                    step.getStatus().name(),
                    step.getAction().map(Enum::name).orElse(null),
                    step.getComment().orElse(null),
                    step.getSlaDeadline(),
                    step.getAssignedAt(),
                    step.getActedAt().orElse(null),
                    step.isEscalated()
            ));
        }

        ApprovalProcessDetail processDetail = new ApprovalProcessDetail(
                process.getId(),
                process.getEntityType().name(),
                process.getEntityId(),
                process.getEntityNumber(),
                process.getStatus().name(),
                process.getCurrentStepIndex(),
                stepsDetailList,
                process.getStartedAt(),
                process.getCompletedAt().orElse(null)
        );

        log.info("[ACTION] Complete GetTaskDetail | taskId={} | actorId={}", taskId, actorId);
        return new ApprovalTaskDetail(taskSummary, processDetail, process.getEntitySnapshot());
    }

    private Optional<ApprovalProcess> findRunningProcess(String taskId) {
        try {
            return approvalProcessRepository.findRunningByStepId(UUID.fromString(taskId));
        } catch (IllegalArgumentException ignored) {
            return approvalProcessRepository.findRunningByCamundaTaskId(taskId);
        }
    }

    private UUID resolveStepId(ApprovalProcess process, String taskId) {
        try {
            return UUID.fromString(taskId);
        } catch (IllegalArgumentException ignored) {
            return process.getSteps().stream()
                    .filter(step -> step.getCamundaTaskId().filter(taskId::equals).isPresent())
                    .map(ApprovalStep::getId)
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(ErrorCode.APR_004));
        }
    }
}
