package com.eprocure.inventory.domain.repository;

import com.eprocure.inventory.domain.model.GoodsReceipt;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoodsReceiptRepository {
    Optional<GoodsReceipt> findById(UUID id);

    Optional<GoodsReceipt> findByIdempotencyKey(UUID idempotencyKey);

    List<GoodsReceipt> findByFilter(GoodsReceiptFilter filter);

    long countByFilter(GoodsReceiptFilter filter);

    Optional<WarehouseSnapshot> findActiveWarehouseById(UUID warehouseId);

    String nextGrNumber(int fiscalYear);

    void insert(GoodsReceipt goodsReceipt);

    record WarehouseSnapshot(UUID id, String name) {
    }
}
