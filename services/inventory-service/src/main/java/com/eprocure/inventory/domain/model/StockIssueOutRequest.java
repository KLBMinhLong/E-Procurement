package com.eprocure.inventory.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record StockIssueOutRequest(
        UUID id,
        UUID idempotencyKey,
        UUID warehouseId,
        UUID prId,
        UUID recipientId,
        UUID issuedBy,
        Instant issuedAt,
        String notes) {

    public StockIssueOutRequest {
        id = Objects.requireNonNull(id, "id must not be null");
        idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        warehouseId = Objects.requireNonNull(warehouseId, "warehouseId must not be null");
        recipientId = Objects.requireNonNull(recipientId, "recipientId must not be null");
        issuedBy = Objects.requireNonNull(issuedBy, "issuedBy must not be null");
        issuedAt = Objects.requireNonNull(issuedAt, "issuedAt must not be null");
        notes = normalizeNullable(notes);
    }

    public static StockIssueOutRequest create(
            UUID idempotencyKey,
            UUID warehouseId,
            UUID prId,
            UUID recipientId,
            UUID issuedBy,
            Instant issuedAt,
            String notes) {
        return new StockIssueOutRequest(
                UUID.randomUUID(),
                idempotencyKey,
                warehouseId,
                prId,
                recipientId,
                issuedBy,
                issuedAt,
                notes);
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
