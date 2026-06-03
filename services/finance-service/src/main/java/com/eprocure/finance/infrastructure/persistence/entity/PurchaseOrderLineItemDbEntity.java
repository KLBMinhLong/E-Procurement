package com.eprocure.finance.infrastructure.persistence.entity;

import com.eprocure.finance.domain.model.PurchaseOrderLineItem;
import java.math.BigDecimal;
import java.util.UUID;

public class PurchaseOrderLineItemDbEntity {
    private UUID id;
    private UUID poId;
    private Integer lineNumber;
    private UUID rfqLineItemId;
    private UUID prLineItemId;
    private String itemName;
    private String categoryCode;
    private BigDecimal quantity;
    private String unit;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String currency;
    private Integer deliveryDays;
    private String warranty;

    public static PurchaseOrderLineItemDbEntity from(UUID poId, PurchaseOrderLineItem lineItem) {
        PurchaseOrderLineItemDbEntity entity = new PurchaseOrderLineItemDbEntity();
        entity.id = lineItem.id();
        entity.poId = poId;
        entity.lineNumber = lineItem.lineNumber();
        entity.rfqLineItemId = lineItem.rfqLineItemId();
        entity.prLineItemId = lineItem.prLineItemId();
        entity.itemName = lineItem.itemName();
        entity.categoryCode = lineItem.categoryCode();
        entity.quantity = lineItem.quantity();
        entity.unit = lineItem.unit();
        entity.unitPrice = lineItem.unitPrice().amount();
        entity.totalPrice = lineItem.totalPrice().amount();
        entity.currency = lineItem.totalPrice().currency();
        entity.deliveryDays = lineItem.deliveryDays();
        entity.warranty = lineItem.warranty();
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPoId() {
        return poId;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public UUID getRfqLineItemId() {
        return rfqLineItemId;
    }

    public UUID getPrLineItemId() {
        return prLineItemId;
    }

    public String getItemName() {
        return itemName;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public String getUnit() {
        return unit;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public String getCurrency() {
        return currency;
    }

    public Integer getDeliveryDays() {
        return deliveryDays;
    }

    public String getWarranty() {
        return warranty;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setPoId(UUID poId) {
        this.poId = poId;
    }

    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }

    public void setRfqLineItemId(UUID rfqLineItemId) {
        this.rfqLineItemId = rfqLineItemId;
    }

    public void setPrLineItemId(UUID prLineItemId) {
        this.prLineItemId = prLineItemId;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setDeliveryDays(Integer deliveryDays) {
        this.deliveryDays = deliveryDays;
    }

    public void setWarranty(String warranty) {
        this.warranty = warranty;
    }
}
