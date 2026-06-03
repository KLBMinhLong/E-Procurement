package com.eprocure.inventory.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record StockEntry(
        String itemCode,
        String itemName,
        UUID warehouseId,
        String warehouseName,
        BigDecimal quantityOnHand,
        String unit,
        BigDecimal reorderPoint,
        boolean belowReorder,
        Instant lastUpdated) {

    public StockEntry {
        itemCode = requireText(itemCode, "itemCode");
        itemName = requireText(itemName, "itemName");
        warehouseId = Objects.requireNonNull(warehouseId, "warehouseId must not be null");
        warehouseName = requireText(warehouseName, "warehouseName");
        quantityOnHand = requireNonNegative(quantityOnHand, "quantityOnHand");
        unit = requireText(unit, "unit");
        if (reorderPoint != null && reorderPoint.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("reorderPoint must not be negative");
        }
        lastUpdated = Objects.requireNonNull(lastUpdated, "lastUpdated must not be null");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static BigDecimal requireNonNegative(BigDecimal value, String fieldName) {
        BigDecimal checked = Objects.requireNonNull(value, fieldName + " must not be null");
        if (checked.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
        return checked;
    }
}
