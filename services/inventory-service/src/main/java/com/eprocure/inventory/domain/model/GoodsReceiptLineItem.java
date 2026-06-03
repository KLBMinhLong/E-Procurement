package com.eprocure.inventory.domain.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record GoodsReceiptLineItem(
        UUID id,
        UUID poLineItemId,
        String itemCode,
        String itemName,
        BigDecimal orderedQuantity,
        BigDecimal receivedQuantity,
        BigDecimal rejectedQuantity,
        String unit,
        String rejectionReason,
        String lotNumber) {

    public GoodsReceiptLineItem {
        id = Objects.requireNonNull(id, "id must not be null");
        poLineItemId = Objects.requireNonNull(poLineItemId, "poLineItemId must not be null");
        itemCode = normalizeNullable(itemCode);
        itemName = requireText(itemName, "itemName");
        orderedQuantity = requirePositive(orderedQuantity, "orderedQuantity");
        receivedQuantity = requireNonNegative(receivedQuantity, "receivedQuantity");
        rejectedQuantity = rejectedQuantity == null ? BigDecimal.ZERO : requireNonNegative(rejectedQuantity, "rejectedQuantity");
        unit = requireText(unit, "unit");
        rejectionReason = normalizeNullable(rejectionReason);
        lotNumber = normalizeNullable(lotNumber);
        if (receivedQuantity.add(rejectedQuantity).compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("received or rejected quantity must be greater than zero");
        }
    }

    public BigDecimal inspectedQuantity() {
        return receivedQuantity.add(rejectedQuantity);
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

    private static BigDecimal requirePositive(BigDecimal value, String fieldName) {
        BigDecimal checked = Objects.requireNonNull(value, fieldName + " must not be null");
        if (checked.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero");
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
