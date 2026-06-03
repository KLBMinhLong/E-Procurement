package com.eprocure.inventory.presentation.response;

import com.eprocure.inventory.domain.model.GoodsReceiptStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GoodsReceiptResponse(
        UUID id,
        String grNumber,
        PoSnapshotResponse po,
        WarehouseSnapshotResponse warehouse,
        WarehouseKeeperSnapshotResponse warehouseKeeper,
        Instant receivedAt,
        GoodsReceiptStatus status,
        List<GoodsReceiptLineItemResponse> lineItems,
        String notes,
        Instant createdAt) {
}
