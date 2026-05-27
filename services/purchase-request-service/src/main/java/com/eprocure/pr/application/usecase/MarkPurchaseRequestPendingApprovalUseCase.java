package com.eprocure.pr.application.usecase;

import com.eprocure.pr.application.port.in.MarkPurchaseRequestPendingApprovalCommand;
import com.eprocure.pr.application.service.IdempotencyService;
import com.eprocure.pr.application.service.MarkedPendingApprovalView;
import com.eprocure.pr.common.exception.BusinessException;
import com.eprocure.pr.common.exception.ErrorCode;
import com.eprocure.pr.common.util.LogMaskingUtil;
import com.eprocure.pr.domain.model.PrStatus;
import com.eprocure.pr.domain.model.PurchaseRequest;
import com.eprocure.pr.domain.repository.PurchaseRequestRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarkPurchaseRequestPendingApprovalUseCase {
    private static final Logger log = LogManager.getLogger(MarkPurchaseRequestPendingApprovalUseCase.class);
    private static final String IDEMPOTENCY_OPERATION = "mark-pr-pending-approval";
    private static final UUID INTERNAL_ACTOR_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final IdempotencyService idempotencyService;
    private final Clock clock;

    public MarkPurchaseRequestPendingApprovalUseCase(
            PurchaseRequestRepository purchaseRequestRepository,
            IdempotencyService idempotencyService,
            Clock clock) {
        this.purchaseRequestRepository = purchaseRequestRepository;
        this.idempotencyService = idempotencyService;
        this.clock = clock;
    }

    @Transactional
    public MarkedPendingApprovalView execute(MarkPurchaseRequestPendingApprovalCommand command, String idempotencyKey) {
        Objects.requireNonNull(command, "command must not be null");
        idempotencyService.verify(idempotencyKey);
        var cached = idempotencyService.find(
                IDEMPOTENCY_OPERATION,
                INTERNAL_ACTOR_ID,
                idempotencyKey,
                MarkedPendingApprovalView.class);
        if (cached.isPresent()) {
            log.info("[ACTION] Idempotency hit MarkPurchaseRequestPendingApproval | prId={} | key={}",
                    LogMaskingUtil.maskId(command.purchaseRequestId()),
                    LogMaskingUtil.maskToken(idempotencyKey));
            return cached.get();
        }

        log.info("[ACTION] Start MarkPurchaseRequestPendingApproval | prId={} | approvalProcessId={}",
                LogMaskingUtil.maskId(command.purchaseRequestId()),
                LogMaskingUtil.maskId(command.approvalProcessId()));
        PurchaseRequest purchaseRequest = purchaseRequestRepository.findById(command.purchaseRequestId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PR_001));

        if (purchaseRequest.getStatus() == PrStatus.SUBMITTED) {
            purchaseRequest.markPendingApproval(Instant.now(clock));
            purchaseRequestRepository.update(purchaseRequest);
        } else if (purchaseRequest.getStatus() != PrStatus.PENDING_APPROVAL) {
            throw new BusinessException(ErrorCode.PR_003);
        }

        MarkedPendingApprovalView view = new MarkedPendingApprovalView(
                purchaseRequest.getId(),
                purchaseRequest.getPrNumber(),
                purchaseRequest.getStatus(),
                command.approvalProcessId(),
                command.camundaProcessInstanceId());
        idempotencyService.save(IDEMPOTENCY_OPERATION, INTERNAL_ACTOR_ID, idempotencyKey, view);
        log.info("[ACTION] Complete MarkPurchaseRequestPendingApproval | prId={} | status={}",
                LogMaskingUtil.maskId(purchaseRequest.getId()),
                purchaseRequest.getStatus());
        return view;
    }
}
