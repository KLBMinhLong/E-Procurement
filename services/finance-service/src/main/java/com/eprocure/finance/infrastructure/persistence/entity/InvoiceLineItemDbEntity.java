package com.eprocure.finance.infrastructure.persistence.entity;

import com.eprocure.finance.domain.model.InvoiceLineItem;
import java.math.BigDecimal;
import java.util.UUID;

public class InvoiceLineItemDbEntity {
    private UUID id;
    private UUID invoiceId;
    private int lineNumber;
    private UUID poLineItemId;
    private String description;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal totalPrice;
    private String currency;

    public static InvoiceLineItemDbEntity from(UUID invoiceId, InvoiceLineItem lineItem) {
        InvoiceLineItemDbEntity entity = new InvoiceLineItemDbEntity();
        entity.id = lineItem.id();
        entity.invoiceId = invoiceId;
        entity.lineNumber = lineItem.lineNumber();
        entity.poLineItemId = lineItem.poLineItemId();
        entity.description = lineItem.description();
        entity.quantity = lineItem.quantity();
        entity.unitPrice = lineItem.unitPrice().amount();
        entity.taxRate = lineItem.taxRate();
        entity.taxAmount = lineItem.taxAmount().amount();
        entity.totalPrice = lineItem.totalPrice().amount();
        entity.currency = lineItem.totalPrice().currency();
        return entity;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getInvoiceId() { return invoiceId; }
    public void setInvoiceId(UUID invoiceId) { this.invoiceId = invoiceId; }
    public int getLineNumber() { return lineNumber; }
    public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }
    public UUID getPoLineItemId() { return poLineItemId; }
    public void setPoLineItemId(UUID poLineItemId) { this.poLineItemId = poLineItemId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
