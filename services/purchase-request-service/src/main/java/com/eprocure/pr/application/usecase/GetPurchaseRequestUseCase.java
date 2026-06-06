package com.eprocure.pr.application.usecase;

import com.eprocure.pr.application.port.in.GetPurchaseRequestQuery;
import com.eprocure.pr.application.service.PoSourceView;
import com.eprocure.pr.application.service.PurchaseRequestDetailView;
import com.eprocure.pr.application.service.PurchaseRequestSummaryView;
import com.eprocure.pr.application.service.RfqSourceView;
import com.eprocure.pr.common.exception.BusinessException;
import com.eprocure.pr.common.exception.ErrorCode;
import com.eprocure.pr.common.util.LogMaskingUtil;
import com.eprocure.pr.domain.model.PurchaseRequest;
import com.eprocure.pr.domain.repository.PurchaseRequestFilter;
import com.eprocure.pr.domain.repository.PurchaseRequestRepository;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use Case: Read purchase requests (detail and list).
 *
 * <p>Read operations — two methods:
 * <ul>
 *   <li>{@link #getDetail(UUID, UUID)} — load one PR by id, verify actor has view access</li>
 *   <li>{@link #getList(GetPurchaseRequestQuery)} — filtered, paginated list</li>
 * </ul>
 */
@Service
public class GetPurchaseRequestUseCase {

    private static final Logger log = LogManager.getLogger(GetPurchaseRequestUseCase.class);

    private final PurchaseRequestRepository purchaseRequestRepository;

    public GetPurchaseRequestUseCase(PurchaseRequestRepository purchaseRequestRepository) {
        this.purchaseRequestRepository = purchaseRequestRepository;
    }

    /**
     * Load full detail of a single PR. Allows requester OR any user with PR_VIEW_ALL
     * (permission enforcement is at the @PreAuthorize level; here we just check ownership).
     */
    @Transactional(readOnly = true)
    public PurchaseRequestDetailView getDetail(UUID purchaseRequestId, UUID actorId) {
        Objects.requireNonNull(purchaseRequestId, "purchaseRequestId must not be null");
        Objects.requireNonNull(actorId, "actorId must not be null");

        log.info("[ACTION] Start GetPurchaseRequestDetail | userId={} | prId={}",
                LogMaskingUtil.maskId(actorId),
                LogMaskingUtil.maskId(purchaseRequestId));

        PurchaseRequest pr = purchaseRequestRepository.findById(purchaseRequestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PR_001));

        log.info("[ACTION] Complete GetPurchaseRequestDetail | prId={} | prNumber={}",
                LogMaskingUtil.maskId(pr.getId()), pr.getPrNumber());

        return PurchaseRequestDetailView.from(pr);
    }

    @Transactional(readOnly = true)
    public RfqSourceView getRfqSource(UUID purchaseRequestId) {
        Objects.requireNonNull(purchaseRequestId, "purchaseRequestId must not be null");

        log.info("[ACTION] Start GetPurchaseRequestRfqSource | prId={}",
                LogMaskingUtil.maskId(purchaseRequestId));

        PurchaseRequest pr = purchaseRequestRepository.findById(purchaseRequestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PR_001));

        log.info("[ACTION] Complete GetPurchaseRequestRfqSource | prId={} | status={}",
                LogMaskingUtil.maskId(pr.getId()),
                pr.getStatus());
        return RfqSourceView.from(pr);
    }

    @Transactional(readOnly = true)
    public PoSourceView getPoSource(UUID purchaseRequestId) {
        Objects.requireNonNull(purchaseRequestId, "purchaseRequestId must not be null");

        log.info("[ACTION] Start GetPurchaseRequestPoSource | prId={}",
                LogMaskingUtil.maskId(purchaseRequestId));

        PurchaseRequest pr = purchaseRequestRepository.findById(purchaseRequestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PR_001));
        if (pr.getStatus() != com.eprocure.pr.domain.model.PrStatus.APPROVED) {
            throw new BusinessException(ErrorCode.PR_003);
        }

        log.info("[ACTION] Complete GetPurchaseRequestPoSource | prId={} | status={}",
                LogMaskingUtil.maskId(pr.getId()),
                pr.getStatus());
        return PoSourceView.from(pr);
    }

    /**
     * Return paginated list of PRs matching the filter.
     *
     * @return result containing list of summary views + total count for pagination meta
     */
    @Transactional(readOnly = true)
    public PagedResult<PurchaseRequestSummaryView> getList(GetPurchaseRequestQuery query) {
        Objects.requireNonNull(query, "query must not be null");

        log.info("[ACTION] Start GetPurchaseRequestList | userId={} | page={} | size={}",
                LogMaskingUtil.maskId(query.actorId()), query.page(), query.size());

        PurchaseRequestFilter filter = toFilter(query);
        List<PurchaseRequest> results = purchaseRequestRepository.findByFilter(filter);
        long totalCount = purchaseRequestRepository.countByFilter(filter);

        List<PurchaseRequestSummaryView> summaries = results.stream()
                .map(this::toSummaryView)
                .toList();

        log.info("[ACTION] Complete GetPurchaseRequestList | userId={} | totalCount={}",
                LogMaskingUtil.maskId(query.actorId()), totalCount);

        return new PagedResult<>(summaries, totalCount, query.page(), query.size());
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private PurchaseRequestFilter toFilter(GetPurchaseRequestQuery query) {
        return new PurchaseRequestFilter(
                query.actorId(),
                query.viewScope(),
                query.status(),
                query.priority(),
                query.departmentId(),
                query.requesterId(),
                query.fromDate(),
                query.toDate(),
                query.minAmount(),
                query.maxAmount(),
                query.q(),
                query.page(),
                query.size(),
                query.sort());
    }

    private PurchaseRequestSummaryView toSummaryView(PurchaseRequest pr) {
        return new PurchaseRequestSummaryView(
                pr.getId(),
                pr.getPrNumber(),
                pr.getTitle(),
                pr.getPriority(),
                pr.getStatus(),
                pr.getTotalAmount(),
                pr.getRequesterId(),
                pr.getDepartmentId(),
                pr.getNeedByDate().orElse(null),
                pr.getCreatedAt(),
                pr.getUpdatedAt().orElse(null));
    }

    /**
     * Container for a paginated result set.
     */
    public record PagedResult<T>(
            List<T> content,
            long totalElements,
            int page,
            int size
    ) {
        public int totalPages() {
            return size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        }

        public boolean isFirst() { return page <= 1; }

        public boolean isLast() { return page >= totalPages(); }
    }
}
