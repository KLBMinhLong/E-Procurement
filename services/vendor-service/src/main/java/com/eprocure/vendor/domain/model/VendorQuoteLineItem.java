package com.eprocure.vendor.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record VendorQuoteLineItem(
        UUID id,
        UUID rfqLineItemId,
        String itemName,
        BigDecimal quantity,
        BigDecimal unitPrice,
        String currency,
        BigDecimal totalPrice,
        Integer deliveryDays,
        String warranty) {

    public VendorQuoteLineItem {
        id = Objects.requireNonNull(id, "id must not be null");
        rfqLineItemId = Objects.requireNonNull(rfqLineItemId, "rfqLineItemId must not be null");
        itemName = requireText(itemName, "itemName");
        quantity = normalizePositive(quantity, "quantity");
        unitPrice = normalizePositive(unitPrice, "unitPrice");
        currency = requireCurrency(currency);
        totalPrice = totalPrice == null ? unitPrice.multiply(quantity).setScale(4, RoundingMode.HALF_UP)
                : normalizePositive(totalPrice, "totalPrice");
        if (deliveryDays != null && deliveryDays < 0) {
            throw new IllegalArgumentException("deliveryDays must not be negative");
        }
        warranty = normalizeNullable(warranty);
    }

    public static VendorQuoteLineItem create(
            UUID rfqLineItemId,
            String itemName,
            BigDecimal quantity,
            BigDecimal unitPrice,
            String currency,
            Integer deliveryDays,
            String warranty) {
        return new VendorQuoteLineItem(
                UUID.randomUUID(),
                rfqLineItemId,
                itemName,
                quantity,
                unitPrice,
                currency,
                null,
                deliveryDays,
                warranty);
    }

    private static BigDecimal normalizePositive(BigDecimal value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.signum() <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private static String requireCurrency(String value) {
        String currency = requireText(value, "currency").toUpperCase(Locale.ROOT);
        if (currency.length() != 3) {
            throw new IllegalArgumentException("currency must be ISO 4217 code");
        }
        return currency;
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
