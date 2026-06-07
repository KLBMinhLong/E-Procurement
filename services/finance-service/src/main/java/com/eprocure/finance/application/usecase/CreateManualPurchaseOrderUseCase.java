package com.eprocure.finance.application.usecase;

import com.eprocure.finance.application.port.in.CreateManualPurchaseOrderCommand;
import com.eprocure.finance.application.port.out.PurchaseRequestPoSourcePort;
import com.eprocure.finance.application.port.out.VendorPoSourcePort;
import com.eprocure.finance.application.service.IdempotencyService;
import com.eprocure.finance.application.service.PoPrConversionCallbackDispatcher;
import com.eprocure.finance.application.service.PurchaseOrderActionResult;
import com.eprocure.finance.application.service.PurchaseOrderView;
import com.eprocure.finance.application.service.PurchaseOrderViewAssembler;
import com.eprocure.finance.application.service.PurchaseRequestPoSource;
import com.eprocure.finance.application.service.VendorPoSource;
import com.eprocure.finance.common.exception.BusinessException;
import com.eprocure.finance.common.exception.ErrorCode;
import com.eprocure.finance.common.util.LogMaskingUtil;
import com.eprocure.finance.domain.model.PoPrConversionCallback;
import com.eprocure.finance.domain.model.PurchaseOrder;
import com.eprocure.finance.domain.model.PurchaseOrderLineItem;
import com.eprocure.finance.domain.model.PurchaseOrderStatus;
import com.eprocure.finance.domain.repository.PoPrConversionCallbackRepository;
import com.eprocure.finance.domain.repository.PurchaseOrderRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class CreateManualPurchaseOrderUseCase {
    private static final Logger log = LogManager.getLogger(CreateManualPurchaseOrderUseCase.class);
    private static final String IDEMPOTENCY_OPERATION = "po-create-manual";
    private static final String APPROVED = "APPROVED";

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PoPrConversionCallbackRepository callbackRepository;
    private final PurchaseRequestPoSourcePort purchaseRequestPoSourcePort;
    private final VendorPoSourcePort vendorPoSourcePort;
    private final PurchaseOrderViewAssembler viewAssembler;
    private final PoPrConversionCallbackDispatcher callbackDispatcher;
    private final IdempotencyService idempotencyService;
    private final Clock clock;

    public CreateManualPurchaseOrderUseCase(
            PurchaseOrderRepository purchaseOrderRepository,
            PoPrConversionCallbackRepository callbackRepository,
            PurchaseRequestPoSourcePort purchaseRequestPoSourcePort,
            VendorPoSourcePort vendorPoSourcePort,
            PurchaseOrderViewAssembler viewAssembler,
            PoPrConversionCallbackDispatcher callbackDispatcher,
            IdempotencyService idempotencyService,
            Clock clock) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.callbackRepository = callbackRepository;
        this.purchaseRequestPoSourcePort = purchaseRequestPoSourcePort;
        this.vendorPoSourcePort = vendorPoSourcePort;
        this.viewAssembler = viewAssembler;
        this.callbackDispatcher = callbackDispatcher;
        this.idempotencyService = idempotencyService;
        this.clock = clock;
    }

    @Transactional
    public PurchaseOrderActionResult execute(CreateManualPurchaseOrderCommand command, String idempotencyKey) {
        Objects.requireNonNull(command, "command must not be null");
        idempotencyService.verify(idempotencyKey);
        var cached = idempotencyService.find(
                IDEMPOTENCY_OPERATION,
                command.actorId(),
                idempotencyKey,
                PurchaseOrderView.class);
        if (cached.isPresent()) {
            log.info("[ACTION] Idempotency hit CreateManualPurchaseOrder | prId={} | userId={} | key={}",
                    LogMaskingUtil.maskId(command.prId()),
                    LogMaskingUtil.maskId(command.actorId()),
                    LogMaskingUtil.maskToken(idempotencyKey));
            return PurchaseOrderActionResult.replayed(cached.get());
        }

        String sourceEventId = "manual-po:" + idempotencyKey;
        var durableReplay = purchaseOrderRepository.findBySourceEventId(sourceEventId);
        if (durableReplay.isPresent()) {
            PurchaseOrderView view = viewAssembler.toView(durableReplay.get());
            idempotencyService.save(IDEMPOTENCY_OPERATION, command.actorId(), idempotencyKey, view);
            return PurchaseOrderActionResult.replayed(view);
        }

        log.info("[ACTION] Start CreateManualPurchaseOrder | prId={} | vendorId={} | userId={}",
                LogMaskingUtil.maskId(command.prId()),
                LogMaskingUtil.maskId(command.vendorId()),
                LogMaskingUtil.maskId(command.actorId()));
        PurchaseRequestPoSource prSource = purchaseRequestPoSourcePort.fetch(command.prId());
        VendorPoSource vendorSource = vendorPoSourcePort.fetch(command.vendorId());
        verifySources(command, prSource, vendorSource);

        purchaseOrderRepository.findActiveManualByPrId(command.prId())
                .ifPresent(existing -> {
                    throw new BusinessException(ErrorCode.FIN_018);
                });

        Instant createdAt = Instant.now(clock);
        UUID idempotencyUuid = UUID.fromString(idempotencyKey);
        PurchaseOrder purchaseOrder = new PurchaseOrder(
                UUID.randomUUID(),
                purchaseOrderRepository.nextPoNumber(createdAt.atZone(ZoneOffset.UTC).getYear()),
                prSource.id(),
                prSource.prNumber(),
                null,
                null,
                null,
                vendorSource.id(),
                vendorSource.name(),
                vendorSource.email(),
                vendorSource.taxCode(),
                command.actorId(),
                command.purchasingOfficerFullName(),
                PurchaseOrderStatus.DRAFT,
                toLineItems(prSource.lineItems()),
                prSource.totalAmount(),
                command.deliveryAddress(),
                command.deliveryDeadline(),
                command.paymentTerms(),
                command.notes(),
                null,
                null,
                null,
                null,
                null,
                createdAt,
                sourceEventId);
        purchaseOrderRepository.insert(purchaseOrder);

        PoPrConversionCallback callback = PoPrConversionCallback.pending(
                purchaseOrder.id(),
                prSource.id(),
                idempotencyUuid,
                createdAt);
        callbackRepository.insert(callback);
        dispatchAfterCommit(callback.id());

        PurchaseOrderView view = viewAssembler.toView(purchaseOrder);
        idempotencyService.save(IDEMPOTENCY_OPERATION, command.actorId(), idempotencyKey, view);
        log.info("[ACTION] Complete CreateManualPurchaseOrder | prId={} | poId={} | poNumber={}",
                LogMaskingUtil.maskId(prSource.id()),
                LogMaskingUtil.maskId(purchaseOrder.id()),
                purchaseOrder.poNumber());
        return PurchaseOrderActionResult.fresh(view);
    }

    private void verifySources(
            CreateManualPurchaseOrderCommand command,
            PurchaseRequestPoSource prSource,
            VendorPoSource vendorSource) {
        if (!command.prId().equals(prSource.id()) || !APPROVED.equals(prSource.status())) {
            throw new BusinessException(ErrorCode.FIN_017);
        }
        if (prSource.lineItems().isEmpty()) {
            throw new BusinessException(ErrorCode.FIN_017);
        }
        if (!command.vendorId().equals(vendorSource.id())
                || !APPROVED.equals(vendorSource.status())
                || !vendorSource.onApprovedVendorList()) {
            throw new BusinessException(ErrorCode.FIN_019);
        }
    }

    private List<PurchaseOrderLineItem> toLineItems(List<PurchaseRequestPoSource.LineItem> sourceLines) {
        return sourceLines.stream()
                .map(item -> new PurchaseOrderLineItem(
                        UUID.randomUUID(),
                        item.lineNumber(),
                        null,
                        item.id(),
                        item.itemName(),
                        item.categoryCode(),
                        item.quantity().amount(),
                        item.quantity().unit(),
                        item.unitPrice(),
                        item.totalPrice(),
                        null,
                        null))
                .toList();
    }

    private void dispatchAfterCommit(UUID callbackId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                callbackDispatcher.dispatch(callbackId);
            }
        });
    }
}
