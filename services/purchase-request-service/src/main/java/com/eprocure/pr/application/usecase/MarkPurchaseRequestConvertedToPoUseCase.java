package com.eprocure.pr.application.usecase;

import com.eprocure.pr.application.port.in.MarkPurchaseRequestConvertedToPoCommand;
import com.eprocure.pr.application.service.ConvertedToPoView;
import com.eprocure.pr.application.service.IdempotencyService;
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
public class MarkPurchaseRequestConvertedToPoUseCase {
    private static final Logger log = LogManager.getLogger(MarkPurchaseRequestConvertedToPoUseCase.class);
    private static final String IDEMPOTENCY_OPERATION = "mark-pr-converted-to-po";
    private static final UUID INTERNAL_ACTOR_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final IdempotencyService idempotencyService;
    private final Clock clock;

    public MarkPurchaseRequestConvertedToPoUseCase(
            PurchaseRequestRepository purchaseRequestRepository,
            IdempotencyService idempotencyService,
            Clock clock) {
        this.purchaseRequestRepository = purchaseRequestRepository;
        this.idempotencyService = idempotencyService;
        this.clock = clock;
    }

    @Transactional
    public ConvertedToPoView execute(MarkPurchaseRequestConvertedToPoCommand command, String idempotencyKey) {
        Objects.requireNonNull(command, "command must not be null");
        idempotencyService.verify(idempotencyKey);
        var cached = idempotencyService.find(
                IDEMPOTENCY_OPERATION,
                INTERNAL_ACTOR_ID,
                idempotencyKey,
                ConvertedToPoView.class);
        if (cached.isPresent()) {
            log.info("[ACTION] Idempotency hit MarkPurchaseRequestConvertedToPo | prId={} | key={}",
                    LogMaskingUtil.maskId(command.purchaseRequestId()),
                    LogMaskingUtil.maskToken(idempotencyKey));
            return cached.get();
        }

        log.info("[ACTION] Start MarkPurchaseRequestConvertedToPo | prId={} | poId={}",
                LogMaskingUtil.maskId(command.purchaseRequestId()),
                LogMaskingUtil.maskId(command.poId()));
        PurchaseRequest purchaseRequest = purchaseRequestRepository.findById(command.purchaseRequestId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PR_001));

        if (purchaseRequest.getStatus() != PrStatus.APPROVED) {
            throw new BusinessException(ErrorCode.PR_003);
        }

        purchaseRequest.convertToPurchaseOrder(Instant.now(clock));
        purchaseRequestRepository.update(purchaseRequest);

        ConvertedToPoView view = new ConvertedToPoView(
                purchaseRequest.getId(),
                purchaseRequest.getPrNumber(),
                purchaseRequest.getStatus(),
                command.poId(),
                command.poNumber());
        idempotencyService.save(IDEMPOTENCY_OPERATION, INTERNAL_ACTOR_ID, idempotencyKey, view);
        log.info("[ACTION] Complete MarkPurchaseRequestConvertedToPo | prId={} | poId={}",
                LogMaskingUtil.maskId(purchaseRequest.getId()),
                LogMaskingUtil.maskId(command.poId()));
        return view;
    }
}
