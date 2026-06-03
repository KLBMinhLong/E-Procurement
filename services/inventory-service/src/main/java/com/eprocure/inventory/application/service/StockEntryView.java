package com.eprocure.inventory.application.service;

import com.eprocure.inventory.domain.model.StockEntry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StockEntryView(
        String itemCode,
        String itemName,
        UUID warehouseId,
        String warehouseName,
        BigDecimal quantityOnHand,
        String unit,
        BigDecimal reorderPoint,
        boolean belowReorder,
        Instant lastUpdated) {

    public static StockEntryView from(StockEntry stockEntry) {
        return new StockEntryView(
                stockEntry.itemCode(),
                stockEntry.itemName(),
                stockEntry.warehouseId(),
                stockEntry.warehouseName(),
                stockEntry.quantityOnHand(),
                stockEntry.unit(),
                stockEntry.reorderPoint(),
                stockEntry.belowReorder(),
                stockEntry.lastUpdated());
    }
}
