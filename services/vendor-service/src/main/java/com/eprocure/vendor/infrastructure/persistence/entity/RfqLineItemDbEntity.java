package com.eprocure.vendor.infrastructure.persistence.entity;

import com.eprocure.vendor.domain.model.RfqLineItem;
import java.math.BigDecimal;
import java.util.UUID;

public class RfqLineItemDbEntity {
    private UUID id;
    private UUID rfqId;
    private UUID prLineItemId;
    private String itemName;
    private String categoryCode;
    private BigDecimal quantity;
    private String unit;
    private String specifications;
    private UUID createdBy;

    public static RfqLineItemDbEntity from(UUID rfqId, RfqLineItem item, UUID actorId) {
        RfqLineItemDbEntity entity = new RfqLineItemDbEntity();
        entity.id = item.id();
        entity.rfqId = rfqId;
        entity.prLineItemId = item.prLineItemId();
        entity.itemName = item.itemName();
        entity.categoryCode = item.categoryCode();
        entity.quantity = item.quantity();
        entity.unit = item.unit();
        entity.specifications = item.specifications();
        entity.createdBy = actorId;
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRfqId() {
        return rfqId;
    }

    public void setRfqId(UUID rfqId) {
        this.rfqId = rfqId;
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

    public String getSpecifications() {
        return specifications;
    }

    public void setSpecifications(String specifications) {
        this.specifications = specifications;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }
}
