package com.eprocure.pr.infrastructure.persistence.entity;

import com.eprocure.pr.domain.model.vo.Money;
import com.eprocure.pr.domain.model.vo.Quantity;
import java.math.BigDecimal;
import java.util.UUID;

public class PrLineItemDbEntity {
    private UUID id;
    private UUID purchaseRequestId;
    private int lineNumber;
    private String itemCode;
    private String itemName;
    private String description;
    private String categoryCode;
    private Quantity quantity;
    private Money unitPrice;
    private Money totalPrice;
    private UUID preferredVendorId;
    private String specifications;
    private String glAccountCode;
    private boolean fromCatalog;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPurchaseRequestId() {
        return purchaseRequestId;
    }

    public void setPurchaseRequestId(UUID purchaseRequestId) {
        this.purchaseRequestId = purchaseRequestId;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    public Quantity getQuantity() {
        return quantity;
    }

    public void setQuantity(Quantity quantity) {
        this.quantity = quantity;
    }

    public void setQuantityAmount(BigDecimal amount) {
        this.quantity = new Quantity(amount, quantity == null ? "unit" : quantity.unit());
    }

    public void setQuantityUnit(String unit) {
        this.quantity = new Quantity(quantity == null ? BigDecimal.ONE : quantity.amount(), unit);
    }

    public Money getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Money unitPrice) {
        this.unitPrice = unitPrice;
    }

    public void setUnitPriceAmount(BigDecimal amount) {
        this.unitPrice = new Money(amount, unitPrice == null ? "VND" : unitPrice.currency());
    }

    public void setUnitPriceCurrency(String currency) {
        this.unitPrice = new Money(unitPrice == null ? BigDecimal.ZERO : unitPrice.amount(), currency);
    }

    public Money getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Money totalPrice) {
        this.totalPrice = totalPrice;
    }

    public void setTotalPriceAmount(BigDecimal amount) {
        this.totalPrice = new Money(amount, totalPrice == null ? "VND" : totalPrice.currency());
    }

    public void setTotalPriceCurrency(String currency) {
        this.totalPrice = new Money(totalPrice == null ? BigDecimal.ZERO : totalPrice.amount(), currency);
    }

    public UUID getPreferredVendorId() {
        return preferredVendorId;
    }

    public void setPreferredVendorId(UUID preferredVendorId) {
        this.preferredVendorId = preferredVendorId;
    }

    public String getSpecifications() {
        return specifications;
    }

    public void setSpecifications(String specifications) {
        this.specifications = specifications;
    }

    public String getGlAccountCode() {
        return glAccountCode;
    }

    public void setGlAccountCode(String glAccountCode) {
        this.glAccountCode = glAccountCode;
    }

    public boolean isFromCatalog() {
        return fromCatalog;
    }

    public void setFromCatalog(boolean fromCatalog) {
        this.fromCatalog = fromCatalog;
    }
}
