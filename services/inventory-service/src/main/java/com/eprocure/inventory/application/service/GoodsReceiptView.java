package com.eprocure.inventory.application.service;

import com.eprocure.inventory.domain.model.GoodsReceipt;
import com.eprocure.inventory.domain.model.GoodsReceiptStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GoodsReceiptView(
        UUID id,
        String grNumber,
        PoSnapshot po,
        WarehouseSnapshot warehouse,
        WarehouseKeeperSnapshot warehouseKeeper,
        Instant receivedAt,
        GoodsReceiptStatus status,
        List<GoodsReceiptLineItemView> lineItems,
        String notes,
        Instant createdAt) {

    public static GoodsReceiptView from(GoodsReceipt goodsReceipt) {
        return new GoodsReceiptView(
                goodsReceipt.id(),
                goodsReceipt.grNumber(),
                new PoSnapshot(goodsReceipt.poId(), goodsReceipt.poNumber()),
                new WarehouseSnapshot(goodsReceipt.warehouseId(), goodsReceipt.warehouseName()),
                new WarehouseKeeperSnapshot(goodsReceipt.warehouseKeeperId(), goodsReceipt.warehouseKeeperFullName()),
                goodsReceipt.receivedAt(),
                goodsReceipt.status(),
                goodsReceipt.lineItems().stream()
                        .map(GoodsReceiptLineItemView::from)
                        .toList(),
                goodsReceipt.notes(),
                goodsReceipt.createdAt());
    }

    public record PoSnapshot(UUID id, String poNumber) {
    }

    public record WarehouseSnapshot(UUID id, String name) {
    }

    public record WarehouseKeeperSnapshot(UUID id, String fullName) {
    }
}
