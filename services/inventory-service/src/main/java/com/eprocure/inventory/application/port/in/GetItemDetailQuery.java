package com.eprocure.inventory.application.port.in;

import java.util.Objects;
import java.util.UUID;

public record GetItemDetailQuery(UUID actorId, String itemCode) {

    public GetItemDetailQuery {
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        itemCode = requireText(itemCode, "itemCode").toUpperCase();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
