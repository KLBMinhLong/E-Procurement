package com.eprocure.analytics.domain.model.projection;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record GoodsReceiptLineProjection(
        UUID grLineItemId,
        UUID poLineItemId,
        String itemCode,
        String itemName,
        BigDecimal orderedQuantity,
        BigDecimal receivedQuantity,
        BigDecimal rejectedQuantity,
        String unit) {

    public GoodsReceiptLineProjection {
        grLineItemId = Objects.requireNonNull(grLineItemId, "grLineItemId must not be null");
        itemCode = defaultText(itemCode, "UNKNOWN_ITEM");
        itemName = defaultText(itemName, "UNNAMED_ITEM");
        orderedQuantity = zeroIfNull(orderedQuantity);
        receivedQuantity = zeroIfNull(receivedQuantity);
        rejectedQuantity = zeroIfNull(rejectedQuantity);
        unit = defaultText(unit, "EA");
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
