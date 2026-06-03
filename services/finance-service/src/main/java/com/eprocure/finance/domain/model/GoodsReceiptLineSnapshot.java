package com.eprocure.finance.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

public record GoodsReceiptLineSnapshot(
        UUID grLineItemId,
        UUID poLineItemId,
        String itemCode,
        String itemName,
        BigDecimal orderedQuantity,
        BigDecimal receivedQuantity,
        BigDecimal rejectedQuantity,
        String unit) {

    public GoodsReceiptLineSnapshot {
        grLineItemId = Objects.requireNonNull(grLineItemId, "grLineItemId must not be null");
        poLineItemId = Objects.requireNonNull(poLineItemId, "poLineItemId must not be null");
        itemCode = normalizeOptional(itemCode);
        itemName = requireText(itemName, "itemName");
        orderedQuantity = requireNonNegative(orderedQuantity, "orderedQuantity");
        receivedQuantity = requireNonNegative(receivedQuantity, "receivedQuantity");
        rejectedQuantity = rejectedQuantity == null
                ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
                : requireNonNegative(rejectedQuantity, "rejectedQuantity");
        unit = requireText(unit, "unit");
    }

    private static BigDecimal requireNonNegative(BigDecimal value, String fieldName) {
        BigDecimal checked = Objects.requireNonNull(value, fieldName + " must not be null")
                .setScale(4, RoundingMode.HALF_UP);
        if (checked.signum() < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
        return checked;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
