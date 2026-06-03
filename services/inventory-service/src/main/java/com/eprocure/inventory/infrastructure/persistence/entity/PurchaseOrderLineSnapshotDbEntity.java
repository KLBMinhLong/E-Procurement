package com.eprocure.inventory.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.util.UUID;

public class PurchaseOrderLineSnapshotDbEntity {
    private UUID id;
    private UUID snapshotId;
    private UUID poId;
    private UUID poLineItemId;
    private UUID prLineItemId;
    private String itemName;
    private String categoryCode;
    private BigDecimal quantity;
    private String unit;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String currency;
    private UUID createdBy;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(UUID snapshotId) {
        this.snapshotId = snapshotId;
    }

    public UUID getPoId() {
        return poId;
    }

    public void setPoId(UUID poId) {
        this.poId = poId;
    }

    public UUID getPoLineItemId() {
        return poLineItemId;
    }

    public void setPoLineItemId(UUID poLineItemId) {
        this.poLineItemId = poLineItemId;
    }

    public UUID getPrLineItemId() {
        return prLineItemId;
    }

    public void setPrLineItemId(UUID prLineItemId) {
        this.prLineItemId = prLineItemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
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

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }
}
