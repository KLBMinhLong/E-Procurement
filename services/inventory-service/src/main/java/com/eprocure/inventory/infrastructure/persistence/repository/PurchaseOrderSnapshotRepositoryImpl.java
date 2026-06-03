package com.eprocure.inventory.infrastructure.persistence.repository;

import com.eprocure.inventory.domain.model.PurchaseOrderSnapshot;
import com.eprocure.inventory.domain.repository.PurchaseOrderSnapshotRepository;
import com.eprocure.inventory.infrastructure.persistence.entity.PurchaseOrderLineSnapshotDbEntity;
import com.eprocure.inventory.infrastructure.persistence.entity.PurchaseOrderSnapshotDbEntity;
import com.eprocure.inventory.infrastructure.persistence.mapper.PurchaseOrderSnapshotMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class PurchaseOrderSnapshotRepositoryImpl implements PurchaseOrderSnapshotRepository {
    private final PurchaseOrderSnapshotMapper purchaseOrderSnapshotMapper;
    private final ObjectMapper domainObjectMapper;

    public PurchaseOrderSnapshotRepositoryImpl(
            PurchaseOrderSnapshotMapper purchaseOrderSnapshotMapper,
            @Qualifier("domainObjectMapper") ObjectMapper domainObjectMapper) {
        this.purchaseOrderSnapshotMapper = purchaseOrderSnapshotMapper;
        this.domainObjectMapper = domainObjectMapper;
    }

    @Override
    public Optional<PurchaseOrderSnapshot> findByPoId(UUID poId) {
        return purchaseOrderSnapshotMapper.findHeaderByPoId(poId)
                .map(this::toDomain);
    }

    @Override
    public Optional<PurchaseOrderSnapshot> findBySourceEventId(String sourceEventId) {
        return purchaseOrderSnapshotMapper.findHeaderBySourceEventId(sourceEventId)
                .map(this::toDomain);
    }

    @Override
    public boolean existsProcessedEvent(String eventId) {
        return purchaseOrderSnapshotMapper.existsProcessedEvent(eventId);
    }

    @Override
    public void insert(PurchaseOrderSnapshot purchaseOrderSnapshot) {
        PurchaseOrderSnapshotDbEntity header = domainObjectMapper.convertValue(
                purchaseOrderSnapshot,
                PurchaseOrderSnapshotDbEntity.class);
        purchaseOrderSnapshotMapper.insertSnapshot(header);
        purchaseOrderSnapshot.lineItems().stream()
                .map(lineItem -> toLineEntity(purchaseOrderSnapshot, lineItem))
                .forEach(purchaseOrderSnapshotMapper::insertLineItem);
    }

    @Override
    public void markEventProcessed(String eventId, String topic, Integer partitionId, Long offsetValue, String handlerName) {
        purchaseOrderSnapshotMapper.markEventProcessed(eventId, topic, partitionId, offsetValue, handlerName);
    }

    private PurchaseOrderSnapshot toDomain(PurchaseOrderSnapshotDbEntity header) {
        List<PurchaseOrderLineSnapshotDbEntity> lineItems = purchaseOrderSnapshotMapper.findLineItemsBySnapshotId(header.getId());
        return new PurchaseOrderSnapshot(
                header.getId(),
                header.getPoId(),
                header.getPoNumber(),
                header.getPrId(),
                header.getPrNumber(),
                header.getVendorId(),
                header.getVendorName(),
                header.getVendorEmail(),
                header.getVendorTaxCode(),
                header.getPurchasingOfficerId(),
                header.getTotalAmount(),
                header.getCurrency(),
                header.getDeliveryAddress(),
                header.getDeliveryDeadline(),
                header.getPaymentTerms(),
                header.getIssuedAt(),
                header.getSentToVendorAt(),
                header.getCreatedAt(),
                header.getSourceEventId(),
                lineItems.stream().map(this::toDomainLineItem).toList());
    }

    private PurchaseOrderSnapshot.LineItem toDomainLineItem(PurchaseOrderLineSnapshotDbEntity entity) {
        return new PurchaseOrderSnapshot.LineItem(
                entity.getPoLineItemId(),
                entity.getPrLineItemId(),
                entity.getItemName(),
                entity.getCategoryCode(),
                entity.getQuantity(),
                entity.getUnit(),
                entity.getUnitPrice(),
                entity.getTotalPrice(),
                entity.getCurrency());
    }

    private PurchaseOrderLineSnapshotDbEntity toLineEntity(
            PurchaseOrderSnapshot snapshot,
            PurchaseOrderSnapshot.LineItem lineItem) {
        PurchaseOrderLineSnapshotDbEntity entity = domainObjectMapper.convertValue(
                lineItem,
                PurchaseOrderLineSnapshotDbEntity.class);
        entity.setSnapshotId(snapshot.id());
        entity.setPoId(snapshot.poId());
        entity.setCreatedBy(snapshot.purchasingOfficerId());
        return entity;
    }
}
