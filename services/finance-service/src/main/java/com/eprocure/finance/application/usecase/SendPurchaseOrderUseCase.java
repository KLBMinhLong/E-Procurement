package com.eprocure.finance.application.usecase;

import com.eprocure.finance.application.port.in.SendPurchaseOrderCommand;
import com.eprocure.finance.application.port.out.PurchaseOrderEmailEventPublisher;
import com.eprocure.finance.application.port.out.PurchaseOrderIssuedEventPublisher;
import com.eprocure.finance.application.service.IdempotencyService;
import com.eprocure.finance.application.service.PurchaseOrderActionResult;
import com.eprocure.finance.application.service.PurchaseOrderView;
import com.eprocure.finance.application.service.PurchaseOrderViewAssembler;
import com.eprocure.finance.common.exception.BusinessException;
import com.eprocure.finance.common.exception.ErrorCode;
import com.eprocure.finance.common.util.LogMaskingUtil;
import com.eprocure.finance.domain.model.PoPrConversionCallbackStatus;
import com.eprocure.finance.domain.event.PurchaseOrderEmailRequestedEvent;
import com.eprocure.finance.domain.event.PurchaseOrderIssuedEvent;
import com.eprocure.finance.domain.model.PurchaseOrder;
import com.eprocure.finance.domain.repository.PoPrConversionCallbackRepository;
import com.eprocure.finance.domain.repository.PurchaseOrderRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SendPurchaseOrderUseCase {
    private static final Logger log = LogManager.getLogger(SendPurchaseOrderUseCase.class);
    private static final String IDEMPOTENCY_OPERATION = "po-send";

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PoPrConversionCallbackRepository callbackRepository;
    private final PurchaseOrderIssuedEventPublisher issuedEventPublisher;
    private final PurchaseOrderEmailEventPublisher emailEventPublisher;
    private final PurchaseOrderViewAssembler viewAssembler;
    private final IdempotencyService idempotencyService;
    private final Clock clock;

    public SendPurchaseOrderUseCase(
            PurchaseOrderRepository purchaseOrderRepository,
            PoPrConversionCallbackRepository callbackRepository,
            PurchaseOrderIssuedEventPublisher issuedEventPublisher,
            PurchaseOrderEmailEventPublisher emailEventPublisher,
            PurchaseOrderViewAssembler viewAssembler,
            IdempotencyService idempotencyService,
            Clock clock) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.callbackRepository = callbackRepository;
        this.issuedEventPublisher = issuedEventPublisher;
        this.emailEventPublisher = emailEventPublisher;
        this.viewAssembler = viewAssembler;
        this.idempotencyService = idempotencyService;
        this.clock = clock;
    }

    @Transactional
    public PurchaseOrderActionResult execute(SendPurchaseOrderCommand command, String idempotencyKey) {
        Objects.requireNonNull(command, "command must not be null");
        idempotencyService.verify(idempotencyKey);
        var cached = idempotencyService.find(
                IDEMPOTENCY_OPERATION,
                command.actorId(),
                idempotencyKey,
                PurchaseOrderView.class);
        if (cached.isPresent()) {
            log.info("[ACTION] Idempotency hit SendPurchaseOrder | poId={} | userId={} | key={}",
                    LogMaskingUtil.maskId(command.purchaseOrderId()),
                    LogMaskingUtil.maskId(command.actorId()),
                    LogMaskingUtil.maskToken(idempotencyKey));
            return PurchaseOrderActionResult.replayed(cached.get());
        }
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(command.purchaseOrderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FIN_006));
        verifyPrConversionDelivered(purchaseOrder);
        try {
            Instant now = Instant.now(clock);
            PurchaseOrder sent = purchaseOrder.sendToVendor(now, command.additionalNote());
            if (sent == purchaseOrder) {
                return replayCurrent(command, idempotencyKey, purchaseOrder);
            }
            purchaseOrderRepository.updateActionState(sent, command.actorId());
            PurchaseOrderIssuedEvent issuedEvent = PurchaseOrderIssuedEvent.create(sent, now);
            PurchaseOrderEmailRequestedEvent emailEvent = PurchaseOrderEmailRequestedEvent.create(sent, now);
            issuedEventPublisher.publish(issuedEvent);
            emailEventPublisher.publish(emailEvent);
            PurchaseOrderView view = viewAssembler.toView(sent);
            idempotencyService.save(IDEMPOTENCY_OPERATION, command.actorId(), idempotencyKey, view);
            log.info("[ACTION] Complete SendPurchaseOrder | poId={} | poNumber={} | userId={}",
                    LogMaskingUtil.maskId(command.purchaseOrderId()),
                    sent.poNumber(),
                    LogMaskingUtil.maskId(command.actorId()));
            return PurchaseOrderActionResult.fresh(view);
        } catch (IllegalStateException | IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.FIN_011);
        }
    }

    private void verifyPrConversionDelivered(PurchaseOrder purchaseOrder) {
        PoPrConversionCallbackStatus status = callbackRepository.findStatusByPoId(purchaseOrder.id())
                .orElse(null);
        if (status != PoPrConversionCallbackStatus.DELIVERED) {
            throw new BusinessException(ErrorCode.FIN_011);
        }
    }

    private PurchaseOrderActionResult replayCurrent(
            SendPurchaseOrderCommand command,
            String idempotencyKey,
            PurchaseOrder purchaseOrder) {
        PurchaseOrderView view = viewAssembler.toView(purchaseOrder);
        idempotencyService.save(IDEMPOTENCY_OPERATION, command.actorId(), idempotencyKey, view);
        return PurchaseOrderActionResult.replayed(view);
    }
}
