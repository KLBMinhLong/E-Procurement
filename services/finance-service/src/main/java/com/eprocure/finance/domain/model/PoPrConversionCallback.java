package com.eprocure.finance.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PoPrConversionCallback(
        UUID id,
        UUID poId,
        UUID prId,
        UUID idempotencyKey,
        PoPrConversionCallbackStatus status,
        int attempts,
        Instant nextRetryAt,
        Instant deliveredAt,
        String lastError,
        Instant createdAt) {

    public PoPrConversionCallback {
        id = Objects.requireNonNull(id, "id must not be null");
        poId = Objects.requireNonNull(poId, "poId must not be null");
        prId = Objects.requireNonNull(prId, "prId must not be null");
        idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        status = Objects.requireNonNull(status, "status must not be null");
        if (attempts < 0) {
            throw new IllegalArgumentException("attempts must not be negative");
        }
        lastError = lastError == null || lastError.isBlank() ? null : lastError.trim();
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static PoPrConversionCallback pending(UUID poId, UUID prId, UUID idempotencyKey, Instant createdAt) {
        return new PoPrConversionCallback(
                UUID.randomUUID(),
                poId,
                prId,
                idempotencyKey,
                PoPrConversionCallbackStatus.PENDING,
                0,
                null,
                null,
                null,
                createdAt);
    }
}
