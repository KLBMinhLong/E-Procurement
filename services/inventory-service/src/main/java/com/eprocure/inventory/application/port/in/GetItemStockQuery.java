package com.eprocure.inventory.application.port.in;

import java.util.Objects;
import java.util.UUID;

public record GetItemStockQuery(
        UUID actorId,
        String itemCode,
        UUID warehouseId) {

    public GetItemStockQuery {
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        itemCode = requireText(itemCode, "itemCode");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
