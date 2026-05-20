package com.eprocure.pr.application.usecase;

import com.eprocure.pr.application.port.in.CancelPurchaseRequestCommand;
import com.eprocure.pr.application.service.IdempotencyService;
import com.eprocure.pr.common.exception.BusinessException;
import com.eprocure.pr.common.exception.ErrorCode;
import com.eprocure.pr.common.util.LogMaskingUtil;
import com.eprocure.pr.domain.event.PrCancelledEvent;
import com.eprocure.pr.domain.model.PurchaseRequest;
import com.eprocure.pr.domain.repository.PurchaseRequestRepository;
import com.eprocure.pr.application.port.out.PrCancelledEventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use Case: Cancel a purchase request.
 *
 * <p>Flow:
 * <ol>
 *   <li>Verify idempotency key format</li>
 *   <li>Check idempotency cache (replay — no-op return)</li>
 *   <li>Load PR — throw PR_001 if not found</li>
 *   <li>Validate actor and cancellable status via domain canBeCancelledBy()</li>
 *   <li>Apply domain cancel() — transitions to CANCELLED, raises PrCancelledEvent</li>
 *   <li>Soft-delete PR record in DB</li>
 *   <li>Cache idempotency result</li>
 * </ol>
 */
@Service
public class CancelPurchaseRequestUseCase {

    private static final Logger log = LogManager.getLogger(CancelPurchaseRequestUseCase.class);
    private static final String IDEMPOTENCY_OPERATION = "cancel-pr";
    // Sentinel value cached to mark idempotent cancel already executed
    private static final String CANCELLED_SENTINEL = "CANCELLED";

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final IdempotencyService idempotencyService;
    private final Clock clock;

    private final PrCancelledEventPublisher eventPublisher;

    public CancelPurchaseRequestUseCase(
            PurchaseRequestRepository purchaseRequestRepository,
            IdempotencyService idempotencyService,
            PrCancelledEventPublisher eventPublisher,
            Clock clock) {
        this.purchaseRequestRepository = purchaseRequestRepository;
        this.idempotencyService = idempotencyService;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public void execute(CancelPurchaseRequestCommand command, String idempotencyKey) {
        Objects.requireNonNull(command, "command must not be null");
        idempotencyService.verify(idempotencyKey);

        // 1. Idempotency check
        var cached = idempotencyService.find(
                IDEMPOTENCY_OPERATION,
                command.actorId(),
                idempotencyKey,
                String.class);
        if (cached.isPresent()) {
            log.info("[ACTION] Idempotency hit CancelPurchaseRequest | userId={} | key={}",
                    LogMaskingUtil.maskId(command.actorId()),
                    LogMaskingUtil.maskToken(idempotencyKey));
            return;
        }

        log.info("[ACTION] Start CancelPurchaseRequest | userId={} | prId={}",
                LogMaskingUtil.maskId(command.actorId()),
                LogMaskingUtil.maskId(command.purchaseRequestId()));

        // 2. Load PR
        PurchaseRequest pr = purchaseRequestRepository.findById(command.purchaseRequestId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PR_001));

        // 3. Validate actor and status via domain guard
        if (!pr.getRequesterId().equals(command.actorId())) {
            throw new BusinessException(ErrorCode.IAM_004);
        }
        if (!pr.canBeCancelledBy(command.actorId())) {
            throw new BusinessException(ErrorCode.PR_003);
        }

        log.info("[ACTION] Step cancel | prId={} | prNumber={} | status={}",
                LogMaskingUtil.maskId(pr.getId()), pr.getPrNumber(), pr.getStatus());

        // 4. Apply domain cancel
        Instant now = Instant.now(clock);
        try {
            pr.cancel(command.actorId(), command.reason(), now);
        } catch (IllegalStateException ex) {
            throw new BusinessException(ErrorCode.PR_003);
        }

        // 5. Soft-delete
        purchaseRequestRepository.softDelete(pr.getId(), command.actorId(), now);

        // 6. Publish events
        pr.pullDomainEvents().stream()
                .filter(PrCancelledEvent.class::isInstance)
                .map(PrCancelledEvent.class::cast)
                .forEach(eventPublisher::publish);

        // 7. Cache idempotency
        idempotencyService.save(IDEMPOTENCY_OPERATION, command.actorId(), idempotencyKey, CANCELLED_SENTINEL);

        log.info("[ACTION] Complete CancelPurchaseRequest | userId={} | prId={} | prNumber={}",
                LogMaskingUtil.maskId(command.actorId()),
                LogMaskingUtil.maskId(pr.getId()),
                pr.getPrNumber());
    }
}
