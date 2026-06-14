package com.eprocure.inventory.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Item(
        UUID id,
        String itemCode,
        String name,
        String description,
        String categoryCode,
        String unit,
        BigDecimal unitPrice,
        String currency,
        UUID preferredVendorId,
        BigDecimal reorderPoint,
        boolean active,
        Instant createdAt,
        UUID createdBy,
        UUID updatedBy) {

    public Item {
        id = Objects.requireNonNull(id, "id must not be null");
        itemCode = requireText(itemCode, "itemCode", 20).toUpperCase();
        name = requireText(name, "name", 300);
        description = normalizeNullable(description);
        categoryCode = requireText(categoryCode, "categoryCode", 50).toUpperCase();
        unit = requireText(unit, "unit", 20);
        unitPrice = requireNonNegative(unitPrice, "unitPrice");
        currency = requireText(currency == null ? "VND" : currency, "currency", 3).toUpperCase();
        if (currency.length() != 3) {
            throw new IllegalArgumentException("currency must be 3 characters");
        }
        reorderPoint = reorderPoint == null ? null : requireNonNegative(reorderPoint, "reorderPoint");
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        createdBy = Objects.requireNonNull(createdBy, "createdBy must not be null");
    }

    public static Item create(
            String itemCode,
            String name,
            String description,
            String categoryCode,
            String unit,
            BigDecimal unitPrice,
            String currency,
            UUID preferredVendorId,
            BigDecimal reorderPoint,
            UUID actorId,
            Instant createdAt) {
        return new Item(
                UUID.randomUUID(),
                itemCode,
                name,
                description,
                categoryCode,
                unit,
                unitPrice,
                currency,
                preferredVendorId,
                reorderPoint,
                true,
                createdAt,
                actorId,
                actorId);
    }

    private static String requireText(String value, String fieldName, int maxLength) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " is too long");
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
