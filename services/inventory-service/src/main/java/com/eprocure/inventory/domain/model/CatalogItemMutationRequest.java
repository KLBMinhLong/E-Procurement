package com.eprocure.inventory.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CatalogItemMutationRequest(
        UUID id,
        UUID idempotencyKey,
        String operation,
        String itemCode,
        UUID actorId,
        Instant createdAt) {

    public CatalogItemMutationRequest {
        id = Objects.requireNonNull(id, "id must not be null");
        idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        operation = requireText(operation, "operation").toUpperCase();
        itemCode = requireText(itemCode, "itemCode").toUpperCase();
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static CatalogItemMutationRequest create(
            UUID idempotencyKey,
            String operation,
            String itemCode,
            UUID actorId,
            Instant createdAt) {
        return new CatalogItemMutationRequest(
                UUID.randomUUID(),
                idempotencyKey,
                operation,
                itemCode,
                actorId,
                createdAt);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
