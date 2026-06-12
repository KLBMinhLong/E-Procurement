package com.eprocure.inventory.application.port.in;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record CreateItemCommand(
        UUID actorId,
        String itemCode,
        String name,
        String description,
        String categoryCode,
        String unit,
        BigDecimal unitPrice,
        String currency,
        UUID preferredVendorId,
        BigDecimal reorderPoint) {

    public CreateItemCommand {
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        itemCode = requireText(itemCode, "itemCode").toUpperCase();
        name = requireText(name, "name");
        description = normalizeNullable(description);
        categoryCode = requireText(categoryCode, "categoryCode").toUpperCase();
        unit = requireText(unit, "unit");
        unitPrice = requireNonNegative(unitPrice, "unitPrice");
        currency = normalizeNullable(currency);
        currency = currency == null ? "VND" : currency.toUpperCase();
        preferredVendorId = preferredVendorId;
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
