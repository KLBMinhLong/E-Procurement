package com.eprocure.finance.infrastructure.persistence.entity;

import com.eprocure.finance.domain.model.PoPrConversionCallback;
import com.eprocure.finance.domain.model.PoPrConversionCallbackStatus;
import java.time.Instant;
import java.util.UUID;

public class PoPrConversionCallbackDbEntity {
    private UUID id;
    private UUID poId;
    private UUID prId;
    private UUID idempotencyKey;
    private PoPrConversionCallbackStatus status;
    private int attempts;
    private Instant nextRetryAt;
    private Instant deliveredAt;
    private String lastError;
    private Instant createdAt;

    public static PoPrConversionCallbackDbEntity from(PoPrConversionCallback callback) {
        PoPrConversionCallbackDbEntity entity = new PoPrConversionCallbackDbEntity();
        entity.id = callback.id();
        entity.poId = callback.poId();
        entity.prId = callback.prId();
        entity.idempotencyKey = callback.idempotencyKey();
        entity.status = callback.status();
        entity.attempts = callback.attempts();
        entity.nextRetryAt = callback.nextRetryAt();
        entity.deliveredAt = callback.deliveredAt();
        entity.lastError = callback.lastError();
        entity.createdAt = callback.createdAt();
        return entity;
    }

    public PoPrConversionCallback toDomain() {
        return new PoPrConversionCallback(
                id,
                poId,
                prId,
                idempotencyKey,
                status,
                attempts,
                nextRetryAt,
                deliveredAt,
                lastError,
                createdAt);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPoId() {
        return poId;
    }

    public void setPoId(UUID poId) {
        this.poId = poId;
    }

    public UUID getPrId() {
        return prId;
    }

    public void setPrId(UUID prId) {
        this.prId = prId;
    }

    public UUID getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(UUID idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public PoPrConversionCallbackStatus getStatus() {
        return status;
    }

    public void setStatus(PoPrConversionCallbackStatus status) {
        this.status = status;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public Instant getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(Instant nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Instant deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
