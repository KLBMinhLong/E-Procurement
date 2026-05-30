package com.eprocure.pr.application.usecase;

import com.eprocure.pr.application.port.in.ApplyPurchaseRequestApprovalResultCommand;
import com.eprocure.pr.application.port.out.PrApprovalResultEventPublisher;
import com.eprocure.pr.application.service.AppliedApprovalResultView;
import com.eprocure.pr.application.service.IdempotencyService;
import com.eprocure.pr.common.exception.BusinessException;
import com.eprocure.pr.common.exception.ErrorCode;
import com.eprocure.pr.common.util.LogMaskingUtil;
import com.eprocure.pr.domain.event.PrApprovalResultEvent;
import com.eprocure.pr.domain.model.PrStatus;
import com.eprocure.pr.domain.model.PurchaseRequest;
import com.eprocure.pr.domain.repository.PurchaseRequestRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplyPurchaseRequestApprovalResultUseCase {
    private static final Logger log = LogManager.getLogger(ApplyPurchaseRequestApprovalResultUseCase.class);
    private static final String IDEMPOTENCY_OPERATION = "apply-pr-approval-result";
    private static final UUID INTERNAL_ACTOR_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final IdempotencyService idempotencyService;
    private final PrApprovalResultEventPublisher eventPublisher;
    private final Clock clock;

    public ApplyPurchaseRequestApprovalResultUseCase(
            PurchaseRequestRepository purchaseRequestRepository,
            IdempotencyService idempotencyService,
            PrApprovalResultEventPublisher eventPublisher,
            Clock clock) {
        this.purchaseRequestRepository = purchaseRequestRepository;
        this.idempotencyService = idempotencyService;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public AppliedApprovalResultView execute(ApplyPurchaseRequestApprovalResultCommand command, String idempotencyKey) {
        Objects.requireNonNull(command, "command must not be null");
        idempotencyService.verify(idempotencyKey);
        var cached = idempotencyService.find(
                operation(command.targetStatus()),
                INTERNAL_ACTOR_ID,
                idempotencyKey,
                AppliedApprovalResultView.class);
        if (cached.isPresent()) {
            log.info("[ACTION] Idempotency hit ApplyPurchaseRequestApprovalResult | prId={} | key={}",
                    LogMaskingUtil.maskId(command.purchaseRequestId()),
                    LogMaskingUtil.maskToken(idempotencyKey));
            return cached.get();
        }

        PurchaseRequest purchaseRequest = purchaseRequestRepository.findById(command.purchaseRequestId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PR_001));
        boolean stateChanged = false;
        if (purchaseRequest.getStatus() == PrStatus.PENDING_APPROVAL) {
            apply(purchaseRequest, command.targetStatus(), Instant.now(clock));
            purchaseRequestRepository.update(purchaseRequest);
            stateChanged = true;
        } else if (purchaseRequest.getStatus() != command.targetStatus()) {
            throw new BusinessException(ErrorCode.PR_003);
        }
        if (stateChanged) {
            publishApprovalResultEvents(purchaseRequest.pullDomainEvents());
        }

        AppliedApprovalResultView view = new AppliedApprovalResultView(
                purchaseRequest.getId(),
                purchaseRequest.getPrNumber(),
                purchaseRequest.getStatus(),
                command.approvalProcessId());
        idempotencyService.save(operation(command.targetStatus()), INTERNAL_ACTOR_ID, idempotencyKey, view);
        log.info("[ACTION] Complete ApplyPurchaseRequestApprovalResult | prId={} | status={}",
                LogMaskingUtil.maskId(purchaseRequest.getId()),
                purchaseRequest.getStatus());
        return view;
    }

    private void apply(PurchaseRequest purchaseRequest, PrStatus targetStatus, Instant actedAt) {
        switch (targetStatus) {
            case APPROVED -> purchaseRequest.approve(actedAt);
            case REJECTED -> purchaseRequest.reject(actedAt);
            case CHANGES_REQUESTED -> purchaseRequest.requestChanges(actedAt);
            default -> throw new IllegalArgumentException("Unsupported approval result status");
        }
    }

    private String operation(PrStatus targetStatus) {
        return IDEMPOTENCY_OPERATION + "-" + targetStatus.name().toLowerCase();
    }

    private void publishApprovalResultEvents(List<Object> events) {
        events.stream()
                .filter(PrApprovalResultEvent.class::isInstance)
                .map(PrApprovalResultEvent.class::cast)
                .forEach(eventPublisher::publish);
    }
}
