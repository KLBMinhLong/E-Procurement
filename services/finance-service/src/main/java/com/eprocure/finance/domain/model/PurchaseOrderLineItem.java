package com.eprocure.finance.domain.model;

import com.eprocure.finance.domain.model.vo.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

public record PurchaseOrderLineItem(
        UUID id,
        int lineNumber,
        UUID rfqLineItemId,
        UUID prLineItemId,
        String itemName,
        String categoryCode,
        BigDecimal quantity,
        String unit,
        Money unitPrice,
        Money totalPrice,
        Integer deliveryDays,
        String warranty) {

    public PurchaseOrderLineItem {
        id = Objects.requireNonNull(id, "id must not be null");
        if (lineNumber < 1) {
            throw new IllegalArgumentException("lineNumber must be positive");
        }
        prLineItemId = Objects.requireNonNull(prLineItemId, "prLineItemId must not be null");
        itemName = requireText(itemName, "itemName");
        categoryCode = requireText(categoryCode, "categoryCode");
        quantity = Objects.requireNonNull(quantity, "quantity must not be null").setScale(4, RoundingMode.HALF_UP);
        if (quantity.signum() <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        unit = requireText(unit, "unit");
        unitPrice = Objects.requireNonNull(unitPrice, "unitPrice must not be null");
        totalPrice = Objects.requireNonNull(totalPrice, "totalPrice must not be null");
        if (!unitPrice.currency().equals(totalPrice.currency())) {
            throw new IllegalArgumentException("line item currency mismatch");
        }
        if (deliveryDays != null && deliveryDays < 0) {
            throw new IllegalArgumentException("deliveryDays must not be negative");
        }
        warranty = warranty == null || warranty.isBlank() ? null : warranty.trim();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
