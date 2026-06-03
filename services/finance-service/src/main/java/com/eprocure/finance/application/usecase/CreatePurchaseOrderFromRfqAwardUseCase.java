package com.eprocure.finance.application.usecase;

import com.eprocure.finance.application.port.in.CreatePurchaseOrderFromRfqAwardCommand;
import com.eprocure.finance.application.service.PurchaseOrderView;
import com.eprocure.finance.common.util.LogMaskingUtil;
import com.eprocure.finance.domain.model.PurchaseOrder;
import com.eprocure.finance.domain.model.PurchaseOrderLineItem;
import com.eprocure.finance.domain.model.PurchaseOrderStatus;
import com.eprocure.finance.domain.repository.PurchaseOrderRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreatePurchaseOrderFromRfqAwardUseCase {
    private static final Logger log = LogManager.getLogger(CreatePurchaseOrderFromRfqAwardUseCase.class);
    private static final String HANDLER_NAME = "CreatePurchaseOrderFromRfqAwardUseCase";

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final Clock clock;

    public CreatePurchaseOrderFromRfqAwardUseCase(PurchaseOrderRepository purchaseOrderRepository, Clock clock) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.clock = clock;
    }

    @Transactional
    public Optional<PurchaseOrderView> execute(CreatePurchaseOrderFromRfqAwardCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        if (purchaseOrderRepository.existsProcessedEvent(command.eventId())) {
            log.info("[ACTION] Skip CreatePurchaseOrderFromRfqAward duplicate event | eventId={}", command.eventId());
            return purchaseOrderRepository.findBySourceEventId(command.eventId())
                    .map(PurchaseOrderView::from);
        }
        Optional<PurchaseOrder> existingPo = purchaseOrderRepository.findByRfqId(command.rfqId());
        if (existingPo.isPresent()) {
            markProcessed(command);
            log.info("[ACTION] Skip CreatePurchaseOrderFromRfqAward existing PO | rfqId={} | poId={}",
                    LogMaskingUtil.maskId(command.rfqId()),
                    LogMaskingUtil.maskId(existingPo.get().id()));
            return existingPo.map(PurchaseOrderView::from);
        }

        Instant createdAt = Instant.now(clock);
        String poNumber = purchaseOrderRepository.nextPoNumber(command.occurredAt().atZone(ZoneOffset.UTC).getYear());
        PurchaseOrder purchaseOrder = new PurchaseOrder(
                UUID.randomUUID(),
                poNumber,
                command.prId(),
                command.prNumber(),
                command.rfqId(),
                command.rfqNumber(),
                command.awardedQuoteId(),
                command.vendorId(),
                command.vendorName(),
                command.vendorEmail(),
                command.vendorTaxCode(),
                command.awardedBy(),
                null,
                PurchaseOrderStatus.DRAFT,
                toLineItems(command.lineItems()),
                command.totalAmount(),
                null,
                null,
                command.paymentTerms(),
                null,
                null,
                null,
                null,
                null,
                null,
                createdAt,
                command.eventId());
        purchaseOrderRepository.insert(purchaseOrder);
        markProcessed(command);
        log.info("[ACTION] Complete CreatePurchaseOrderFromRfqAward | rfqId={} | poId={} | poNumber={}",
                LogMaskingUtil.maskId(command.rfqId()),
                LogMaskingUtil.maskId(purchaseOrder.id()),
                purchaseOrder.poNumber());
        return Optional.of(PurchaseOrderView.from(purchaseOrder));
    }

    private List<PurchaseOrderLineItem> toLineItems(List<CreatePurchaseOrderFromRfqAwardCommand.LineItem> commandItems) {
        AtomicInteger lineNumber = new AtomicInteger(1);
        return commandItems.stream()
                .map(item -> new PurchaseOrderLineItem(
                        UUID.randomUUID(),
                        lineNumber.getAndIncrement(),
                        item.rfqLineItemId(),
                        item.prLineItemId(),
                        item.itemName(),
                        item.categoryCode(),
                        item.quantity(),
                        item.unit(),
                        item.unitPrice(),
                        item.totalPrice(),
                        item.deliveryDays(),
                        item.warranty()))
                .toList();
    }

    private void markProcessed(CreatePurchaseOrderFromRfqAwardCommand command) {
        purchaseOrderRepository.markEventProcessed(
                command.eventId(),
                command.topic(),
                command.partitionId(),
                command.offsetValue(),
                HANDLER_NAME);
    }
}
