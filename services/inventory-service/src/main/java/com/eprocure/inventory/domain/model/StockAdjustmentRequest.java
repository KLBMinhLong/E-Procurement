package com.eprocure.inventory.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record StockAdjustmentRequest(
        UUID id,
        UUID idempotencyKey,
        UUID warehouseId,
        String itemCode,
        BigDecimal previousQuantity,
        BigDecimal newQuantity,
        String unit,
        UUID adjustedBy,
        Instant adjustedAt,
        String reason) {

    public StockAdjustmentRequest {
        id = Objects.requireNonNull(id, "id must not be null");
        idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        warehouseId = Objects.requireNonNull(warehouseId, "warehouseId must not be null");
        itemCode = requireText(itemCode, "itemCode");
        previousQuantity = requireNonNegative(previousQuantity, "previousQuantity");
        newQuantity = requireNonNegative(newQuantity, "newQuantity");
        unit = requireText(unit, "unit");
        adjustedBy = Objects.requireNonNull(adjustedBy, "adjustedBy must not be null");
        adjustedAt = Objects.requireNonNull(adjustedAt, "adjustedAt must not be null");
        reason = requireText(reason, "reason");
    }

    public static StockAdjustmentRequest create(
            UUID idempotencyKey,
            UUID warehouseId,
            String itemCode,
            BigDecimal previousQuantity,
            BigDecimal newQuantity,
            String unit,
            UUID adjustedBy,
            Instant adjustedAt,
            String reason) {
        return new StockAdjustmentRequest(
                UUID.randomUUID(),
                idempotencyKey,
                warehouseId,
                itemCode,
                previousQuantity,
                newQuantity,
                unit,
                adjustedBy,
                adjustedAt,
                reason);
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
