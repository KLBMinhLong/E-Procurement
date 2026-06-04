package com.eprocure.analytics.domain.model.projection;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record PoIssuedLineProjection(
        UUID poLineItemId,
        UUID prLineItemId,
        String itemName,
        String categoryCode,
        BigDecimal quantity,
        String unit,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        String currency) {

    public PoIssuedLineProjection {
        poLineItemId = Objects.requireNonNull(poLineItemId, "poLineItemId must not be null");
        itemName = defaultText(itemName, "UNNAMED_ITEM");
        categoryCode = defaultText(categoryCode, "UNCATEGORIZED");
        quantity = zeroIfNull(quantity);
        unit = defaultText(unit, "EA");
        unitPrice = zeroIfNull(unitPrice);
        totalPrice = zeroIfNull(totalPrice);
        currency = normalizeCurrency(currency);
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String normalizeCurrency(String value) {
        String normalized = defaultText(value, "VND").toUpperCase();
        if (normalized.length() != 3) {
            throw new IllegalArgumentException("currency must be a 3-letter code");
        }
        return normalized;
    }
}
