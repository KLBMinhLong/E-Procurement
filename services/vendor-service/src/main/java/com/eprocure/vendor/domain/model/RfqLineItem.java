package com.eprocure.vendor.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record RfqLineItem(
        UUID id,
        UUID prLineItemId,
        String itemName,
        String categoryCode,
        BigDecimal quantity,
        String unit,
        String specifications) {

    public RfqLineItem {
        id = Objects.requireNonNull(id, "id must not be null");
        prLineItemId = Objects.requireNonNull(prLineItemId, "prLineItemId must not be null");
        itemName = requireText(itemName, "itemName");
        categoryCode = requireText(categoryCode, "categoryCode").toUpperCase(Locale.ROOT);
        quantity = normalizeQuantity(quantity);
        unit = requireText(unit, "unit");
        specifications = normalizeNullable(specifications);
    }

    public static RfqLineItem create(
            UUID prLineItemId,
            String itemName,
            String categoryCode,
            BigDecimal quantity,
            String unit,
            String specifications) {
        return new RfqLineItem(UUID.randomUUID(), prLineItemId, itemName, categoryCode, quantity, unit, specifications);
    }

    private static BigDecimal normalizeQuantity(BigDecimal value) {
        Objects.requireNonNull(value, "quantity must not be null");
        if (value.signum() <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        return value.setScale(4, RoundingMode.HALF_UP);
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
}
