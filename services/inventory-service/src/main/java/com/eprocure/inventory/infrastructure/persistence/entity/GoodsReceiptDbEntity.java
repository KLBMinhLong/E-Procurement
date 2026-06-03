package com.eprocure.inventory.infrastructure.persistence.entity;

import com.eprocure.inventory.domain.model.GoodsReceiptStatus;
import java.time.Instant;
import java.util.UUID;

public class GoodsReceiptDbEntity {
    private UUID id;
    private String grNumber;
    private UUID poId;
    private String poNumber;
    private UUID warehouseId;
    private String warehouseName;
    private UUID warehouseKeeperId;
    private String warehouseKeeperFullName;
    private Instant receivedAt;
    private GoodsReceiptStatus status;
    private String notes;
    private Instant createdAt;
    private UUID createdBy;
    private UUID updatedBy;
    private UUID idempotencyKey;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getGrNumber() {
        return grNumber;
    }

    public void setGrNumber(String grNumber) {
        this.grNumber = grNumber;
    }

    public UUID getPoId() {
        return poId;
    }

    public void setPoId(UUID poId) {
        this.poId = poId;
    }

    public String getPoNumber() {
        return poNumber;
    }

    public void setPoNumber(String poNumber) {
        this.poNumber = poNumber;
    }

    public UUID getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(UUID warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public UUID getWarehouseKeeperId() {
        return warehouseKeeperId;
    }

    public void setWarehouseKeeperId(UUID warehouseKeeperId) {
        this.warehouseKeeperId = warehouseKeeperId;
    }

    public String getWarehouseKeeperFullName() {
        return warehouseKeeperFullName;
    }

    public void setWarehouseKeeperFullName(String warehouseKeeperFullName) {
        this.warehouseKeeperFullName = warehouseKeeperFullName;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public GoodsReceiptStatus getStatus() {
        return status;
    }

    public void setStatus(GoodsReceiptStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public UUID getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(UUID idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
