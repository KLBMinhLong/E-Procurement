package com.eprocure.finance.infrastructure.persistence.entity;

import com.eprocure.finance.domain.model.GoodsReceiptSnapshot;
import com.eprocure.finance.domain.model.GoodsReceiptSnapshotStatus;
import java.time.Instant;
import java.util.UUID;

public class GoodsReceiptSnapshotDbEntity {
    private UUID id;
    private String grNumber;
    private UUID poId;
    private String poNumber;
    private UUID warehouseId;
    private UUID warehouseKeeperId;
    private GoodsReceiptSnapshotStatus status;
    private Instant receivedAt;
    private Instant completedAt;
    private String sourceEventId;

    public static GoodsReceiptSnapshotDbEntity from(GoodsReceiptSnapshot snapshot) {
        GoodsReceiptSnapshotDbEntity entity = new GoodsReceiptSnapshotDbEntity();
        entity.id = snapshot.id();
        entity.grNumber = snapshot.grNumber();
        entity.poId = snapshot.poId();
        entity.poNumber = snapshot.poNumber();
        entity.warehouseId = snapshot.warehouseId();
        entity.warehouseKeeperId = snapshot.warehouseKeeperId();
        entity.status = snapshot.status();
        entity.receivedAt = snapshot.receivedAt();
        entity.completedAt = snapshot.completedAt();
        entity.sourceEventId = snapshot.sourceEventId();
        return entity;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getGrNumber() { return grNumber; }
    public void setGrNumber(String grNumber) { this.grNumber = grNumber; }
    public UUID getPoId() { return poId; }
    public void setPoId(UUID poId) { this.poId = poId; }
    public String getPoNumber() { return poNumber; }
    public void setPoNumber(String poNumber) { this.poNumber = poNumber; }
    public UUID getWarehouseId() { return warehouseId; }
    public void setWarehouseId(UUID warehouseId) { this.warehouseId = warehouseId; }
    public UUID getWarehouseKeeperId() { return warehouseKeeperId; }
    public void setWarehouseKeeperId(UUID warehouseKeeperId) { this.warehouseKeeperId = warehouseKeeperId; }
    public GoodsReceiptSnapshotStatus getStatus() { return status; }
    public void setStatus(GoodsReceiptSnapshotStatus status) { this.status = status; }
    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public String getSourceEventId() { return sourceEventId; }
    public void setSourceEventId(String sourceEventId) { this.sourceEventId = sourceEventId; }
}
