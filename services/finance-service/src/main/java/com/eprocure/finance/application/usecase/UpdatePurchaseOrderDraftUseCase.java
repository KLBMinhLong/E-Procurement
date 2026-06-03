package com.eprocure.finance.application.usecase;

import com.eprocure.finance.application.port.in.UpdatePurchaseOrderDraftCommand;
import com.eprocure.finance.application.service.IdempotencyService;
import com.eprocure.finance.application.service.PurchaseOrderActionResult;
import com.eprocure.finance.application.service.PurchaseOrderView;
import com.eprocure.finance.common.exception.BusinessException;
import com.eprocure.finance.common.exception.ErrorCode;
import com.eprocure.finance.common.util.LogMaskingUtil;
import com.eprocure.finance.domain.model.PurchaseOrder;
import com.eprocure.finance.domain.repository.PurchaseOrderRepository;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdatePurchaseOrderDraftUseCase {
    private static final Logger log = LogManager.getLogger(UpdatePurchaseOrderDraftUseCase.class);
    private static final String IDEMPOTENCY_OPERATION = "po-update-draft";

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final IdempotencyService idempotencyService;

    public UpdatePurchaseOrderDraftUseCase(
            PurchaseOrderRepository purchaseOrderRepository,
            IdempotencyService idempotencyService) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.idempotencyService = idempotencyService;
    }

    @Transactional
    public PurchaseOrderActionResult execute(UpdatePurchaseOrderDraftCommand command, String idempotencyKey) {
        Objects.requireNonNull(command, "command must not be null");
        idempotencyService.verify(idempotencyKey);
        var cached = idempotencyService.find(
                IDEMPOTENCY_OPERATION,
                command.actorId(),
                idempotencyKey,
                PurchaseOrderView.class);
        if (cached.isPresent()) {
            log.info("[ACTION] Idempotency hit UpdatePurchaseOrderDraft | poId={} | userId={} | key={}",
                    LogMaskingUtil.maskId(command.purchaseOrderId()),
                    LogMaskingUtil.maskId(command.actorId()),
                    LogMaskingUtil.maskToken(idempotencyKey));
            return PurchaseOrderActionResult.replayed(cached.get());
        }
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(command.purchaseOrderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FIN_006));
        try {
            PurchaseOrder updated = purchaseOrder.updateDraftDetails(
                    command.deliveryAddress(),
                    command.deliveryDeadline(),
                    command.paymentTerms(),
                    command.actorId());
            purchaseOrderRepository.updateDraftDetails(updated, command.actorId());
            PurchaseOrderView view = PurchaseOrderView.from(updated);
            idempotencyService.save(IDEMPOTENCY_OPERATION, command.actorId(), idempotencyKey, view);
            log.info("[ACTION] Complete UpdatePurchaseOrderDraft | poId={} | userId={}",
                    LogMaskingUtil.maskId(command.purchaseOrderId()),
                    LogMaskingUtil.maskId(command.actorId()));
            return PurchaseOrderActionResult.fresh(view);
        } catch (IllegalStateException | IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.FIN_011);
        }
    }
}
