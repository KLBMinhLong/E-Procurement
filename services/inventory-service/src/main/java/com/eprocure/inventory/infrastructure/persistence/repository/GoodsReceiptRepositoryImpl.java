package com.eprocure.inventory.infrastructure.persistence.repository;

import com.eprocure.inventory.domain.model.GoodsReceipt;
import com.eprocure.inventory.domain.model.GoodsReceiptLineItem;
import com.eprocure.inventory.domain.model.GoodsReceiptStatus;
import com.eprocure.inventory.domain.model.StockMovement;
import com.eprocure.inventory.domain.repository.GoodsReceiptFilter;
import com.eprocure.inventory.domain.repository.GoodsReceiptRepository;
import com.eprocure.inventory.infrastructure.persistence.entity.GoodsReceiptDbEntity;
import com.eprocure.inventory.infrastructure.persistence.entity.GoodsReceiptLineItemDbEntity;
import com.eprocure.inventory.infrastructure.persistence.entity.StockMovementDbEntity;
import com.eprocure.inventory.infrastructure.persistence.mapper.GoodsReceiptMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class GoodsReceiptRepositoryImpl implements GoodsReceiptRepository {
    private final GoodsReceiptMapper goodsReceiptMapper;
    private final ObjectMapper domainObjectMapper;

    public GoodsReceiptRepositoryImpl(
            GoodsReceiptMapper goodsReceiptMapper,
            @Qualifier("domainObjectMapper") ObjectMapper domainObjectMapper) {
        this.goodsReceiptMapper = goodsReceiptMapper;
        this.domainObjectMapper = domainObjectMapper;
    }

    @Override
    public Optional<GoodsReceipt> findById(UUID id) {
        return goodsReceiptMapper.findHeaderById(id).map(this::toDomain);
    }

    @Override
    public Optional<GoodsReceipt> findByIdempotencyKey(UUID idempotencyKey) {
        return goodsReceiptMapper.findHeaderByIdempotencyKey(idempotencyKey).map(this::toDomain);
    }

    @Override
    public Optional<GoodsReceipt> findByIdAndCompleteIdempotencyKey(UUID id, UUID idempotencyKey) {
        return goodsReceiptMapper.findHeaderByIdAndCompleteIdempotencyKey(id, idempotencyKey).map(this::toDomain);
    }

    @Override
    public Optional<GoodsReceipt> findByIdAndUpdateIdempotencyKey(UUID id, UUID idempotencyKey) {
        return goodsReceiptMapper.findHeaderByIdAndUpdateIdempotencyKey(id, idempotencyKey).map(this::toDomain);
    }

    @Override
    public List<GoodsReceipt> findByFilter(GoodsReceiptFilter filter) {
        List<GoodsReceiptDbEntity> headers = goodsReceiptMapper.findHeadersByFilter(filter);
        if (headers.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<GoodsReceiptLineItemDbEntity>> lineItems = headers.stream()
                .collect(Collectors.toMap(
                        GoodsReceiptDbEntity::getId,
                        header -> goodsReceiptMapper.findLineItemsByGoodsReceiptId(header.getId())));
        return headers.stream()
                .map(header -> toDomain(header, lineItems.getOrDefault(header.getId(), List.of())))
                .toList();
    }

    @Override
    public long countByFilter(GoodsReceiptFilter filter) {
        return goodsReceiptMapper.countByFilter(filter);
    }

    @Override
    public Optional<WarehouseSnapshot> findActiveWarehouseById(UUID warehouseId) {
        return goodsReceiptMapper.findActiveWarehouseById(warehouseId)
                .map(entity -> new WarehouseSnapshot(entity.getId(), entity.getName()));
    }

    @Override
    public String nextGrNumber(int fiscalYear) {
        return goodsReceiptMapper.nextGrNumber(fiscalYear);
    }

    @Override
    public void insert(GoodsReceipt goodsReceipt) {
        GoodsReceiptDbEntity header = domainObjectMapper.convertValue(goodsReceipt, GoodsReceiptDbEntity.class);
        goodsReceiptMapper.insertHeader(header);
        goodsReceipt.lineItems().stream()
                .map(lineItem -> toLineEntity(goodsReceipt, lineItem))
                .forEach(goodsReceiptMapper::insertLineItem);
    }

    @Override
    public boolean updateDraft(GoodsReceipt goodsReceipt, UUID actorId, Instant updatedAt, UUID idempotencyKey) {
        GoodsReceiptDbEntity header = domainObjectMapper.convertValue(goodsReceipt, GoodsReceiptDbEntity.class);
        int updated = goodsReceiptMapper.updateDraftHeader(header, actorId, updatedAt, idempotencyKey);
        if (updated == 0) {
            return false;
        }
        goodsReceiptMapper.softDeleteLineItemsByGoodsReceiptId(goodsReceipt.id(), actorId, updatedAt);
        goodsReceipt.lineItems().stream()
                .map(lineItem -> toLineEntity(goodsReceipt, lineItem))
                .forEach(goodsReceiptMapper::insertLineItem);
        return true;
    }

    @Override
    public Optional<String> findActiveItemCodeForPoLineItem(UUID poLineItemId) {
        return goodsReceiptMapper.findActiveItemCodeForPoLineItem(poLineItemId);
    }

    @Override
    public void updateLineItemCode(UUID lineItemId, String itemCode, UUID actorId) {
        goodsReceiptMapper.updateLineItemCode(lineItemId, itemCode, actorId);
    }

    @Override
    public BigDecimal receiveStock(
            String itemCode,
            UUID warehouseId,
            BigDecimal quantity,
            String unit,
            UUID actorId,
            Instant occurredAt) {
        return goodsReceiptMapper.receiveStock(itemCode, warehouseId, quantity, unit, actorId, occurredAt);
    }

    @Override
    public void insertStockMovement(StockMovement stockMovement) {
        goodsReceiptMapper.insertStockMovement(
                domainObjectMapper.convertValue(stockMovement, StockMovementDbEntity.class));
    }

    @Override
    public boolean markCompleted(
            UUID id,
            GoodsReceiptStatus status,
            UUID actorId,
            Instant completedAt,
            UUID idempotencyKey) {
        return goodsReceiptMapper.markCompleted(id, status, actorId, completedAt, idempotencyKey) > 0;
    }

    @Override
    public int countReceiptMovements(UUID goodsReceiptId) {
        return goodsReceiptMapper.countReceiptMovements(goodsReceiptId);
    }

    @Override
    public List<StockBalance> findStockBalancesByReceipt(UUID goodsReceiptId) {
        return goodsReceiptMapper.findStockBalancesByReceipt(goodsReceiptId).stream()
                .map(entity -> new StockBalance(entity.getItemCode(), entity.getQuantityOnHand()))
                .toList();
    }

    private GoodsReceipt toDomain(GoodsReceiptDbEntity header) {
        return toDomain(header, goodsReceiptMapper.findLineItemsByGoodsReceiptId(header.getId()));
    }

    private GoodsReceipt toDomain(GoodsReceiptDbEntity header, List<GoodsReceiptLineItemDbEntity> lineItems) {
        return new GoodsReceipt(
                header.getId(),
                header.getGrNumber(),
                header.getPoId(),
                header.getPoNumber(),
                header.getWarehouseId(),
                header.getWarehouseName(),
                header.getWarehouseKeeperId(),
                header.getWarehouseKeeperFullName(),
                header.getReceivedAt(),
                header.getStatus(),
                lineItems.stream().map(this::toDomainLineItem).toList(),
                header.getNotes(),
                header.getCreatedAt(),
                header.getCreatedBy(),
                header.getUpdatedBy(),
                header.getIdempotencyKey());
    }

    private GoodsReceiptLineItem toDomainLineItem(GoodsReceiptLineItemDbEntity entity) {
        return new GoodsReceiptLineItem(
                entity.getId(),
                entity.getPoLineItemId(),
                entity.getItemCode(),
                entity.getItemName(),
                entity.getOrderedQuantity(),
                entity.getReceivedQuantity(),
                entity.getRejectedQuantity(),
                entity.getUnit(),
                entity.getRejectionReason(),
                entity.getLotNumber());
    }

    private GoodsReceiptLineItemDbEntity toLineEntity(GoodsReceipt goodsReceipt, GoodsReceiptLineItem lineItem) {
        GoodsReceiptLineItemDbEntity entity = domainObjectMapper.convertValue(lineItem, GoodsReceiptLineItemDbEntity.class);
        entity.setGoodsReceiptId(goodsReceipt.id());
        entity.setCreatedBy(goodsReceipt.createdBy());
        return entity;
    }
}
