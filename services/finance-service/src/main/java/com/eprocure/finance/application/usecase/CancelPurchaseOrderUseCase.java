package com.eprocure.finance.application.usecase;

import com.eprocure.finance.application.port.in.CancelPurchaseOrderCommand;
import com.eprocure.finance.application.service.IdempotencyService;
import com.eprocure.finance.application.service.PurchaseOrderActionResult;
import com.eprocure.finance.application.service.PurchaseOrderView;
import com.eprocure.finance.common.exception.BusinessException;
import com.eprocure.finance.common.exception.ErrorCode;
import com.eprocure.finance.common.util.LogMaskingUtil;
import com.eprocure.finance.domain.model.PurchaseOrder;
import com.eprocure.finance.domain.repository.PurchaseOrderRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CancelPurchaseOrderUseCase {
    private static final Logger log = LogManager.getLogger(CancelPurchaseOrderUseCase.class);
    private static final String IDEMPOTENCY_OPERATION = "po-cancel";
    private static final int REASON_MIN_LENGTH = 10;

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final IdempotencyService idempotencyService;
    private final Clock clock;

    public CancelPurchaseOrderUseCase(
            PurchaseOrderRepository purchaseOrderRepository,
            IdempotencyService idempotencyService,
            Clock clock) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.idempotencyService = idempotencyService;
        this.clock = clock;
    }

    @Transactional
    public PurchaseOrderActionResult execute(CancelPurchaseOrderCommand command, String idempotencyKey) {
        Objects.requireNonNull(command, "command must not be null");
        idempotencyService.verify(idempotencyKey);
        var cached = idempotencyService.find(
                IDEMPOTENCY_OPERATION,
                command.actorId(),
                idempotencyKey,
                PurchaseOrderView.class);
        if (cached.isPresent()) {
            log.info("[ACTION] Idempotency hit CancelPurchaseOrder | poId={} | userId={} | key={}",
                    LogMaskingUtil.maskId(command.purchaseOrderId()),
                    LogMaskingUtil.maskId(command.actorId()),
                    LogMaskingUtil.maskToken(idempotencyKey));
            return PurchaseOrderActionResult.replayed(cached.get());
        }
        if (command.reason() == null || command.reason().length() < REASON_MIN_LENGTH) {
            throw new BusinessException(ErrorCode.VAL_001);
        }
        PurchaseOrder purchaseOrder = findPurchaseOrder(command.purchaseOrderId());
        try {
            PurchaseOrder cancelled = purchaseOrder.cancel(command.actorId(), Instant.now(clock), command.reason());
            if (cancelled == purchaseOrder) {
                return replayCurrent(command, idempotencyKey, purchaseOrder);
            }
            purchaseOrderRepository.updateActionState(cancelled, command.actorId());
            PurchaseOrderView view = PurchaseOrderView.from(cancelled);
            idempotencyService.save(IDEMPOTENCY_OPERATION, command.actorId(), idempotencyKey, view);
            log.info("[ACTION] Complete CancelPurchaseOrder | poId={} | userId={}",
                    LogMaskingUtil.maskId(command.purchaseOrderId()),
                    LogMaskingUtil.maskId(command.actorId()));
            return PurchaseOrderActionResult.fresh(view);
        } catch (IllegalStateException exception) {
            throw new BusinessException(ErrorCode.FIN_011);
        }
    }

    private PurchaseOrder findPurchaseOrder(java.util.UUID purchaseOrderId) {
        return purchaseOrderRepository.findById(purchaseOrderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FIN_006));
    }

    private PurchaseOrderActionResult replayCurrent(
            CancelPurchaseOrderCommand command,
            String idempotencyKey,
            PurchaseOrder purchaseOrder) {
        PurchaseOrderView view = PurchaseOrderView.from(purchaseOrder);
        idempotencyService.save(IDEMPOTENCY_OPERATION, command.actorId(), idempotencyKey, view);
        return PurchaseOrderActionResult.replayed(view);
    }
}
