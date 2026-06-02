package com.eprocure.vendor.infrastructure.persistence.entity;

import com.eprocure.vendor.domain.model.VendorQuoteLineItem;
import java.math.BigDecimal;
import java.util.UUID;

public class VendorQuoteLineItemDbEntity {
    private UUID id;
    private UUID quoteId;
    private UUID rfqLineItemId;
    private String itemName;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private String currency;
    private BigDecimal totalPrice;
    private Integer deliveryDays;
    private String warranty;
    private UUID createdBy;

    public static VendorQuoteLineItemDbEntity from(UUID quoteId, VendorQuoteLineItem item, UUID actorId) {
        VendorQuoteLineItemDbEntity entity = new VendorQuoteLineItemDbEntity();
        entity.id = item.id();
        entity.quoteId = quoteId;
        entity.rfqLineItemId = item.rfqLineItemId();
        entity.itemName = item.itemName();
        entity.quantity = item.quantity();
        entity.unitPrice = item.unitPrice();
        entity.currency = item.currency();
        entity.totalPrice = item.totalPrice();
        entity.deliveryDays = item.deliveryDays();
        entity.warranty = item.warranty();
        entity.createdBy = actorId;
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getQuoteId() {
        return quoteId;
    }

    public void setQuoteId(UUID quoteId) {
        this.quoteId = quoteId;
    }

    public UUID getRfqLineItemId() {
        return rfqLineItemId;
    }

    public void setRfqLineItemId(UUID rfqLineItemId) {
        this.rfqLineItemId = rfqLineItemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public Integer getDeliveryDays() {
        return deliveryDays;
    }

    public void setDeliveryDays(Integer deliveryDays) {
        this.deliveryDays = deliveryDays;
    }

    public String getWarranty() {
        return warranty;
    }

    public void setWarranty(String warranty) {
        this.warranty = warranty;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }
}
