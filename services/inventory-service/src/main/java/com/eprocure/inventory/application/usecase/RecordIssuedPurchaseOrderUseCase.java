package com.eprocure.inventory.application.usecase;

import com.eprocure.inventory.application.port.in.RecordIssuedPurchaseOrderCommand;
import com.eprocure.inventory.common.util.LogMaskingUtil;
import com.eprocure.inventory.domain.model.PurchaseOrderSnapshot;
import com.eprocure.inventory.domain.repository.PurchaseOrderSnapshotRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecordIssuedPurchaseOrderUseCase {
    private static final Logger log = LogManager.getLogger(RecordIssuedPurchaseOrderUseCase.class);
    private static final String HANDLER_NAME = "RecordIssuedPurchaseOrderUseCase";

    private final PurchaseOrderSnapshotRepository purchaseOrderSnapshotRepository;
    private final Clock clock;

    public RecordIssuedPurchaseOrderUseCase(
            PurchaseOrderSnapshotRepository purchaseOrderSnapshotRepository,
            Clock clock) {
        this.purchaseOrderSnapshotRepository = purchaseOrderSnapshotRepository;
        this.clock = clock;
    }

    @Transactional
    public Optional<PurchaseOrderSnapshot> execute(RecordIssuedPurchaseOrderCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        if (purchaseOrderSnapshotRepository.existsProcessedEvent(command.eventId())) {
            log.info("[ACTION] Skip RecordIssuedPurchaseOrder duplicate event | eventId={}", command.eventId());
            return purchaseOrderSnapshotRepository.findBySourceEventId(command.eventId());
        }

        Optional<PurchaseOrderSnapshot> existingSnapshot = purchaseOrderSnapshotRepository.findByPoId(command.poId());
        if (existingSnapshot.isPresent()) {
            markProcessed(command);
            log.info("[ACTION] Skip RecordIssuedPurchaseOrder existing snapshot | poId={} | snapshotId={}",
                    LogMaskingUtil.maskId(command.poId()),
                    LogMaskingUtil.maskId(existingSnapshot.get().id()));
            return existingSnapshot;
        }

        PurchaseOrderSnapshot snapshot = toSnapshot(command);
        purchaseOrderSnapshotRepository.insert(snapshot);
        markProcessed(command);
        log.info("[ACTION] Complete RecordIssuedPurchaseOrder | poId={} | snapshotId={} | poNumber={}",
                LogMaskingUtil.maskId(snapshot.poId()),
                LogMaskingUtil.maskId(snapshot.id()),
                snapshot.poNumber());
        return Optional.of(snapshot);
    }

    private PurchaseOrderSnapshot toSnapshot(RecordIssuedPurchaseOrderCommand command) {
        Instant createdAt = Instant.now(clock);
        return new PurchaseOrderSnapshot(
                UUID.randomUUID(),
                command.poId(),
                command.poNumber(),
                command.prId(),
                command.prNumber(),
                command.vendorId(),
                command.vendorName(),
                command.vendorEmail(),
                command.vendorTaxCode(),
                command.purchasingOfficerId(),
                command.totalAmount(),
                command.currency(),
                command.deliveryAddress(),
                command.deliveryDeadline(),
                command.paymentTerms(),
                command.issuedAt(),
                command.sentToVendorAt(),
                createdAt,
                command.eventId(),
                command.lineItems().stream()
                        .map(item -> new PurchaseOrderSnapshot.LineItem(
                                item.poLineItemId(),
                                item.prLineItemId(),
                                item.itemName(),
                                item.categoryCode(),
                                item.quantity(),
                                item.unit(),
                                item.unitPrice(),
                                item.totalPrice(),
                                item.currency()))
                        .toList());
    }

    private void markProcessed(RecordIssuedPurchaseOrderCommand command) {
        purchaseOrderSnapshotRepository.markEventProcessed(
                command.eventId(),
                command.topic(),
                command.partitionId(),
                command.offsetValue(),
                HANDLER_NAME);
    }
}
