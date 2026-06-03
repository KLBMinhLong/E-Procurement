package com.eprocure.inventory.presentation.response;

import java.time.Instant;
import java.util.UUID;

public record StockEntryResponse(
        String itemCode,
        String itemName,
        UUID warehouseId,
        String warehouseName,
        String quantityOnHand,
        String unit,
        String reorderPoint,
        boolean isBelowReorder,
        Instant lastUpdated) {
}
