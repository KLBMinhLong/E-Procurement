package com.eprocure.inventory.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

public class StockIssueOutRequestDbEntity {
    private UUID id;
    private UUID idempotencyKey;
    private UUID warehouseId;
    private UUID prId;
    private UUID recipientId;
    private UUID issuedBy;
    private Instant issuedAt;
    private String notes;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(UUID idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public UUID getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(UUID warehouseId) {
        this.warehouseId = warehouseId;
    }

    public UUID getPrId() {
        return prId;
    }

    public void setPrId(UUID prId) {
        this.prId = prId;
    }

    public UUID getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(UUID recipientId) {
        this.recipientId = recipientId;
    }

    public UUID getIssuedBy() {
        return issuedBy;
    }

    public void setIssuedBy(UUID issuedBy) {
        this.issuedBy = issuedBy;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
