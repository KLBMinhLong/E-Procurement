package com.eprocure.inventory.application.port.in;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record IssueOutStockCommand(
        UUID actorId,
        UUID warehouseId,
        UUID prId,
        UUID recipientId,
        List<LineItem> items,
        String notes) {

    public IssueOutStockCommand {
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        warehouseId = Objects.requireNonNull(warehouseId, "warehouseId must not be null");
        recipientId = Objects.requireNonNull(recipientId, "recipientId must not be null");
        items = List.copyOf(Objects.requireNonNull(items, "items must not be null"));
        if (items.isEmpty()) {
            throw new IllegalArgumentException("items must not be empty");
        }
        notes = normalizeNullable(notes);
    }

    public record LineItem(
            String itemCode,
            BigDecimal quantity,
            String unit) {

        public LineItem {
            itemCode = requireText(itemCode, "itemCode");
            quantity = requirePositive(quantity, "quantity");
            unit = requireText(unit, "unit");
        }
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
}
