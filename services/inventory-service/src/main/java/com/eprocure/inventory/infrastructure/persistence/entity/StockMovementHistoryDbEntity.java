package com.eprocure.inventory.infrastructure.persistence.entity;

import com.eprocure.inventory.domain.model.StockMovementType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class StockMovementHistoryDbEntity {
    private UUID id;
    private String itemCode;
    private String itemName;
    private UUID warehouseId;
    private StockMovementType movementType;
    private BigDecimal quantity;
    private String unit;
    private BigDecimal balanceAfter;
    private String sourceRefType;
    private UUID sourceRefId;
    private UUID performedBy;
    private String performedByFullName;
    private Instant performedAt;
    private String notes;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public UUID getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(UUID warehouseId) {
        this.warehouseId = warehouseId;
    }

    public StockMovementType getMovementType() {
        return movementType;
    }

    public void setMovementType(StockMovementType movementType) {
        this.movementType = movementType;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public String getSourceRefType() {
        return sourceRefType;
    }

    public void setSourceRefType(String sourceRefType) {
        this.sourceRefType = sourceRefType;
    }

    public UUID getSourceRefId() {
        return sourceRefId;
    }

    public void setSourceRefId(UUID sourceRefId) {
        this.sourceRefId = sourceRefId;
    }

    public UUID getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(UUID performedBy) {
        this.performedBy = performedBy;
    }

    public String getPerformedByFullName() {
        return performedByFullName;
    }

    public void setPerformedByFullName(String performedByFullName) {
        this.performedByFullName = performedByFullName;
    }

    public Instant getPerformedAt() {
        return performedAt;
    }

    public void setPerformedAt(Instant performedAt) {
        this.performedAt = performedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
