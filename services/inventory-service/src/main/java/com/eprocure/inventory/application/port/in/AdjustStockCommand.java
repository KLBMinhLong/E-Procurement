package com.eprocure.inventory.application.port.in;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record AdjustStockCommand(
        UUID actorId,
        UUID warehouseId,
        String itemCode,
        BigDecimal newQuantity,
        String unit,
        String reason) {

    public AdjustStockCommand {
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        warehouseId = Objects.requireNonNull(warehouseId, "warehouseId must not be null");
        itemCode = requireText(itemCode, "itemCode");
        newQuantity = requireNonNegative(newQuantity, "newQuantity");
        unit = requireText(unit, "unit");
        reason = requireText(reason, "reason");
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
