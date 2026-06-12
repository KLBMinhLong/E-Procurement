package com.eprocure.inventory.domain.repository;

import com.eprocure.inventory.domain.model.GoodsReceipt;
import com.eprocure.inventory.domain.model.GoodsReceiptStatus;
import com.eprocure.inventory.domain.model.StockMovement;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoodsReceiptRepository {
    Optional<GoodsReceipt> findById(UUID id);

    Optional<GoodsReceipt> findByIdempotencyKey(UUID idempotencyKey);

    Optional<GoodsReceipt> findByIdAndCompleteIdempotencyKey(UUID id, UUID idempotencyKey);

    Optional<GoodsReceipt> findByIdAndUpdateIdempotencyKey(UUID id, UUID idempotencyKey);

    List<GoodsReceipt> findByFilter(GoodsReceiptFilter filter);

    long countByFilter(GoodsReceiptFilter filter);

    Optional<WarehouseSnapshot> findActiveWarehouseById(UUID warehouseId);

    String nextGrNumber(int fiscalYear);

    void insert(GoodsReceipt goodsReceipt);

    boolean updateDraft(GoodsReceipt goodsReceipt, UUID actorId, Instant updatedAt, UUID idempotencyKey);

    Optional<String> findActiveItemCodeForPoLineItem(UUID poLineItemId);

    void updateLineItemCode(UUID lineItemId, String itemCode, UUID actorId);

    BigDecimal receiveStock(String itemCode, UUID warehouseId, BigDecimal quantity, String unit, UUID actorId, Instant occurredAt);

    void insertStockMovement(StockMovement stockMovement);

    boolean markCompleted(UUID id, GoodsReceiptStatus status, UUID actorId, Instant completedAt, UUID idempotencyKey);

    int countReceiptMovements(UUID goodsReceiptId);

    List<StockBalance> findStockBalancesByReceipt(UUID goodsReceiptId);

    record WarehouseSnapshot(UUID id, String name) {
    }

    record StockBalance(String itemCode, BigDecimal quantityOnHand) {
    }
}
