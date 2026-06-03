package com.eprocure.inventory.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record StockMovementHistory(
        UUID id,
        String itemCode,
        String itemName,
        UUID warehouseId,
        StockMovementType movementType,
        BigDecimal quantity,
        String unit,
        BigDecimal balanceAfter,
        String sourceRefType,
        UUID sourceRefId,
        UUID performedBy,
        String performedByFullName,
        Instant performedAt,
        String notes) {

    public StockMovementHistory {
        id = Objects.requireNonNull(id, "id must not be null");
        itemCode = requireText(itemCode, "itemCode");
        itemName = requireText(itemName, "itemName");
        warehouseId = Objects.requireNonNull(warehouseId, "warehouseId must not be null");
        movementType = Objects.requireNonNull(movementType, "movementType must not be null");
        quantity = requireNonZero(quantity, "quantity");
        unit = requireText(unit, "unit");
        balanceAfter = requireNonNegative(balanceAfter, "balanceAfter");
        sourceRefType = normalizeNullable(sourceRefType);
        performedBy = Objects.requireNonNull(performedBy, "performedBy must not be null");
        performedByFullName = requireText(performedByFullName, "performedByFullName");
        performedAt = Objects.requireNonNull(performedAt, "performedAt must not be null");
        notes = normalizeNullable(notes);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static BigDecimal requireNonZero(BigDecimal value, String fieldName) {
        BigDecimal checked = Objects.requireNonNull(value, fieldName + " must not be null");
        if (checked.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException(fieldName + " must not be zero");
        }
        return checked;
    }

    private static BigDecimal requireNonNegative(BigDecimal value, String fieldName) {
        BigDecimal checked = Objects.requireNonNull(value, fieldName + " must not be null");
        if (checked.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
        return checked;
    }
}
