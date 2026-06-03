package com.eprocure.finance.infrastructure.persistence.entity;

import com.eprocure.finance.domain.model.GoodsReceiptLineSnapshot;
import java.math.BigDecimal;
import java.util.UUID;

public class GoodsReceiptLineSnapshotDbEntity {
    private UUID grLineItemId;
    private UUID grId;
    private UUID poLineItemId;
    private String itemCode;
    private String itemName;
    private BigDecimal orderedQuantity;
    private BigDecimal receivedQuantity;
    private BigDecimal rejectedQuantity;
    private String unit;

    public static GoodsReceiptLineSnapshotDbEntity from(UUID grId, GoodsReceiptLineSnapshot lineItem) {
        GoodsReceiptLineSnapshotDbEntity entity = new GoodsReceiptLineSnapshotDbEntity();
        entity.grLineItemId = lineItem.grLineItemId();
        entity.grId = grId;
        entity.poLineItemId = lineItem.poLineItemId();
        entity.itemCode = lineItem.itemCode();
        entity.itemName = lineItem.itemName();
        entity.orderedQuantity = lineItem.orderedQuantity();
        entity.receivedQuantity = lineItem.receivedQuantity();
        entity.rejectedQuantity = lineItem.rejectedQuantity();
        entity.unit = lineItem.unit();
        return entity;
    }

    public UUID getGrLineItemId() { return grLineItemId; }
    public void setGrLineItemId(UUID grLineItemId) { this.grLineItemId = grLineItemId; }
    public UUID getGrId() { return grId; }
    public void setGrId(UUID grId) { this.grId = grId; }
    public UUID getPoLineItemId() { return poLineItemId; }
    public void setPoLineItemId(UUID poLineItemId) { this.poLineItemId = poLineItemId; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public BigDecimal getOrderedQuantity() { return orderedQuantity; }
    public void setOrderedQuantity(BigDecimal orderedQuantity) { this.orderedQuantity = orderedQuantity; }
    public BigDecimal getReceivedQuantity() { return receivedQuantity; }
    public void setReceivedQuantity(BigDecimal receivedQuantity) { this.receivedQuantity = receivedQuantity; }
    public BigDecimal getRejectedQuantity() { return rejectedQuantity; }
    public void setRejectedQuantity(BigDecimal rejectedQuantity) { this.rejectedQuantity = rejectedQuantity; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
}
