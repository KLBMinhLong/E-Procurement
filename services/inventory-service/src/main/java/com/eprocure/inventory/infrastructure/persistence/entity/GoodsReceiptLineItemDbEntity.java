package com.eprocure.inventory.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.util.UUID;

public class GoodsReceiptLineItemDbEntity {
    private UUID id;
    private UUID goodsReceiptId;
    private UUID poLineItemId;
    private String itemCode;
    private String itemName;
    private BigDecimal orderedQuantity;
    private BigDecimal receivedQuantity;
    private BigDecimal rejectedQuantity;
    private String unit;
    private String rejectionReason;
    private String lotNumber;
    private UUID createdBy;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getGoodsReceiptId() {
        return goodsReceiptId;
    }

    public void setGoodsReceiptId(UUID goodsReceiptId) {
        this.goodsReceiptId = goodsReceiptId;
    }

    public UUID getPoLineItemId() {
        return poLineItemId;
    }

    public void setPoLineItemId(UUID poLineItemId) {
        this.poLineItemId = poLineItemId;
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

    public BigDecimal getOrderedQuantity() {
        return orderedQuantity;
    }

    public void setOrderedQuantity(BigDecimal orderedQuantity) {
        this.orderedQuantity = orderedQuantity;
    }

    public BigDecimal getReceivedQuantity() {
        return receivedQuantity;
    }

    public void setReceivedQuantity(BigDecimal receivedQuantity) {
        this.receivedQuantity = receivedQuantity;
    }

    public BigDecimal getRejectedQuantity() {
        return rejectedQuantity;
    }

    public void setRejectedQuantity(BigDecimal rejectedQuantity) {
        this.rejectedQuantity = rejectedQuantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getLotNumber() {
        return lotNumber;
    }

    public void setLotNumber(String lotNumber) {
        this.lotNumber = lotNumber;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }
}
