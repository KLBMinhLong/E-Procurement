package com.eprocure.finance.infrastructure.persistence.repository;

import com.eprocure.finance.domain.model.PurchaseOrder;
import com.eprocure.finance.domain.model.PurchaseOrderLineItem;
import com.eprocure.finance.domain.model.vo.Money;
import com.eprocure.finance.domain.repository.PurchaseOrderFilter;
import com.eprocure.finance.domain.repository.PurchaseOrderRepository;
import com.eprocure.finance.infrastructure.persistence.entity.PurchaseOrderDbEntity;
import com.eprocure.finance.infrastructure.persistence.entity.PurchaseOrderLineItemDbEntity;
import com.eprocure.finance.infrastructure.persistence.mapper.PurchaseOrderMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class PurchaseOrderRepositoryImpl implements PurchaseOrderRepository {
    private final PurchaseOrderMapper purchaseOrderMapper;

    public PurchaseOrderRepositoryImpl(PurchaseOrderMapper purchaseOrderMapper) {
        this.purchaseOrderMapper = purchaseOrderMapper;
    }

    @Override
    public Optional<PurchaseOrder> findById(UUID poId) {
        return purchaseOrderMapper.findHeaderById(poId)
                .map(this::toDomain);
    }

    @Override
    public Optional<PurchaseOrder> findBySourceEventId(String sourceEventId) {
        return purchaseOrderMapper.findHeaderBySourceEventId(sourceEventId)
                .map(this::toDomain);
    }

    @Override
    public Optional<PurchaseOrder> findByRfqId(UUID rfqId) {
        return purchaseOrderMapper.findHeaderByRfqId(rfqId)
                .map(this::toDomain);
    }

    @Override
    public Optional<PurchaseOrder> findActiveManualByPrId(UUID prId) {
        return purchaseOrderMapper.findActiveManualHeaderByPrId(prId)
                .map(this::toDomain);
    }

    @Override
    public List<PurchaseOrder> findByFilter(PurchaseOrderFilter filter) {
        List<PurchaseOrderDbEntity> headers = purchaseOrderMapper.findHeadersByFilter(filter);
        if (headers.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<PurchaseOrderLineItemDbEntity>> lineItems = purchaseOrderMapper.findLineItemsByPoIds(
                        headers.stream().map(PurchaseOrderDbEntity::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(PurchaseOrderLineItemDbEntity::getPoId));
        return headers.stream()
                .map(header -> toDomain(header, lineItems.getOrDefault(header.getId(), List.of())))
                .toList();
    }

    @Override
    public long countByFilter(PurchaseOrderFilter filter) {
        return purchaseOrderMapper.countByFilter(filter);
    }

    @Override
    public String nextPoNumber(int fiscalYear) {
        return purchaseOrderMapper.nextPoNumber(fiscalYear);
    }

    @Override
    public void insert(PurchaseOrder purchaseOrder) {
        purchaseOrderMapper.insertPurchaseOrder(PurchaseOrderDbEntity.from(purchaseOrder));
        purchaseOrder.lineItems().stream()
                .map(lineItem -> PurchaseOrderLineItemDbEntity.from(purchaseOrder.id(), lineItem))
                .forEach(entity -> purchaseOrderMapper.insertLineItem(entity, purchaseOrder.purchasingOfficerId()));
    }

    @Override
    public void updateDraftDetails(PurchaseOrder purchaseOrder, UUID actorId) {
        purchaseOrderMapper.updateDraftDetails(PurchaseOrderDbEntity.from(purchaseOrder), actorId);
    }

    @Override
    public void updateActionState(PurchaseOrder purchaseOrder, UUID actorId) {
        purchaseOrderMapper.updateActionState(PurchaseOrderDbEntity.from(purchaseOrder), actorId);
    }

    @Override
    public boolean existsProcessedEvent(String eventId) {
        return purchaseOrderMapper.existsProcessedEvent(eventId);
    }

    @Override
    public void markEventProcessed(String eventId, String topic, Integer partitionId, Long offsetValue, String handlerName) {
        purchaseOrderMapper.markEventProcessed(eventId, topic, partitionId, offsetValue, handlerName);
    }

    private PurchaseOrder toDomain(PurchaseOrderDbEntity header) {
        return toDomain(header, purchaseOrderMapper.findLineItemsByPoId(header.getId()));
    }

    private PurchaseOrder toDomain(PurchaseOrderDbEntity header, List<PurchaseOrderLineItemDbEntity> lineItems) {
        return new PurchaseOrder(
                header.getId(),
                header.getPoNumber(),
                header.getPrId(),
                header.getPrNumber(),
                header.getRfqId(),
                header.getRfqNumber(),
                header.getAwardedQuoteId(),
                header.getVendorId(),
                header.getVendorName(),
                header.getVendorEmail(),
                header.getVendorTaxCode(),
                header.getPurchasingOfficerId(),
                header.getPurchasingOfficerFullName(),
                header.getStatus(),
                lineItems.stream()
                        .map(this::toDomainLineItem)
                        .toList(),
                new Money(header.getTotalAmount(), header.getCurrency()),
                header.getDeliveryAddress(),
                header.getDeliveryDeadline(),
                header.getPaymentTerms(),
                header.getVendorNote(),
                header.getIssuedAt(),
                header.getSentToVendorAt(),
                header.getCancelledAt(),
                header.getCancelledBy(),
                header.getCancelReason(),
                header.getCreatedAt(),
                header.getSourceEventId());
    }

    private PurchaseOrderLineItem toDomainLineItem(PurchaseOrderLineItemDbEntity entity) {
        return new PurchaseOrderLineItem(
                entity.getId(),
                entity.getLineNumber(),
                entity.getRfqLineItemId(),
                entity.getPrLineItemId(),
                entity.getItemName(),
                entity.getCategoryCode(),
                entity.getQuantity(),
                entity.getUnit(),
                new Money(entity.getUnitPrice(), entity.getCurrency()),
                new Money(entity.getTotalPrice(), entity.getCurrency()),
                entity.getDeliveryDays(),
                entity.getWarranty());
    }
}
