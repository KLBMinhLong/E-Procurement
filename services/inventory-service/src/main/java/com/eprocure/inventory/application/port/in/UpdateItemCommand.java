package com.eprocure.inventory.application.port.in;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record UpdateItemCommand(
        UUID actorId,
        String itemCode,
        String name,
        String description,
        BigDecimal unitPrice,
        String currency,
        UUID preferredVendorId,
        BigDecimal reorderPoint,
        Boolean active) {

    public UpdateItemCommand {
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        itemCode = requireText(itemCode, "itemCode").toUpperCase();
        name = normalizeNullable(name);
        description = normalizeNullable(description);
        unitPrice = unitPrice == null ? null : requireNonNegative(unitPrice, "unitPrice");
        currency = normalizeNullable(currency);
        if (currency != null) {
            currency = currency.toUpperCase();
        }
        reorderPoint = reorderPoint == null ? null : requireNonNegative(reorderPoint, "reorderPoint");
    }

    private static String requireText(String value, String fieldName) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static BigDecimal requireNonNegative(BigDecimal value, String fieldName) {
        BigDecimal checked = Objects.requireNonNull(value, fieldName + " must not be null");
        if (checked.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
        return checked;
    }
}
