package com.eprocure.pr.domain.model.vo;

import java.util.Objects;

public record InventoryStockItem(
        String itemCode,
        String itemName,
        Quantity quantityOnHand) {

    public InventoryStockItem {
        itemCode = requireText(itemCode, "itemCode");
        itemName = requireText(itemName, "itemName");
        quantityOnHand = Objects.requireNonNull(quantityOnHand, "quantityOnHand must not be null");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
