package com.eprocure.finance.infrastructure.persistence.entity;

import com.eprocure.finance.domain.model.Invoice;
import com.eprocure.finance.domain.model.InvoiceStatus;
import com.eprocure.finance.domain.model.MatchStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class InvoiceDbEntity {
    private UUID id;
    private String invoiceNumber;
    private UUID vendorId;
    private String vendorName;
    private UUID poId;
    private String poNumber;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String currency;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private InvoiceStatus status;
    private MatchStatus poMatchStatus;
    private MatchStatus grMatchStatus;
    private BigDecimal qtyVariance;
    private BigDecimal priceVariance;
    private Instant matchedAt;
    private UUID matchedBy;
    private UUID approvedBy;
    private Instant approvedAt;
    private Instant createdAt;
    private UUID createdBy;
    private UUID idempotencyKey;
    private UUID matchIdempotencyKey;

    public static InvoiceDbEntity from(Invoice invoice) {
        InvoiceDbEntity entity = new InvoiceDbEntity();
        entity.id = invoice.id();
        entity.invoiceNumber = invoice.invoiceNumber();
        entity.vendorId = invoice.vendorId();
        entity.vendorName = invoice.vendorName();
        entity.poId = invoice.poId();
        entity.poNumber = invoice.poNumber();
        entity.subtotal = invoice.subtotal().amount();
        entity.taxAmount = invoice.taxAmount().amount();
        entity.totalAmount = invoice.totalAmount().amount();
        entity.currency = invoice.totalAmount().currency();
        entity.invoiceDate = invoice.invoiceDate();
        entity.dueDate = invoice.dueDate();
        entity.status = invoice.status();
        entity.poMatchStatus = invoice.poMatchStatus();
        entity.grMatchStatus = invoice.grMatchStatus();
        entity.qtyVariance = invoice.qtyVariance();
        entity.priceVariance = invoice.priceVariance() == null ? null : invoice.priceVariance().amount();
        entity.matchedAt = invoice.matchedAt();
        entity.matchedBy = invoice.matchedBy();
        entity.approvedBy = invoice.approvedBy();
        entity.approvedAt = invoice.approvedAt();
        entity.createdAt = invoice.createdAt();
        entity.createdBy = invoice.createdBy();
        entity.idempotencyKey = invoice.idempotencyKey();
        entity.matchIdempotencyKey = invoice.matchIdempotencyKey();
        return entity;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public UUID getVendorId() { return vendorId; }
    public void setVendorId(UUID vendorId) { this.vendorId = vendorId; }
    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }
    public UUID getPoId() { return poId; }
    public void setPoId(UUID poId) { this.poId = poId; }
    public String getPoNumber() { return poNumber; }
    public void setPoNumber(String poNumber) { this.poNumber = poNumber; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public LocalDate getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(LocalDate invoiceDate) { this.invoiceDate = invoiceDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public InvoiceStatus getStatus() { return status; }
    public void setStatus(InvoiceStatus status) { this.status = status; }
    public MatchStatus getPoMatchStatus() { return poMatchStatus; }
    public void setPoMatchStatus(MatchStatus poMatchStatus) { this.poMatchStatus = poMatchStatus; }
    public MatchStatus getGrMatchStatus() { return grMatchStatus; }
    public void setGrMatchStatus(MatchStatus grMatchStatus) { this.grMatchStatus = grMatchStatus; }
    public BigDecimal getQtyVariance() { return qtyVariance; }
    public void setQtyVariance(BigDecimal qtyVariance) { this.qtyVariance = qtyVariance; }
    public BigDecimal getPriceVariance() { return priceVariance; }
    public void setPriceVariance(BigDecimal priceVariance) { this.priceVariance = priceVariance; }
    public Instant getMatchedAt() { return matchedAt; }
    public void setMatchedAt(Instant matchedAt) { this.matchedAt = matchedAt; }
    public UUID getMatchedBy() { return matchedBy; }
    public void setMatchedBy(UUID matchedBy) { this.matchedBy = matchedBy; }
    public UUID getApprovedBy() { return approvedBy; }
    public void setApprovedBy(UUID approvedBy) { this.approvedBy = approvedBy; }
    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public UUID getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(UUID idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public UUID getMatchIdempotencyKey() { return matchIdempotencyKey; }
    public void setMatchIdempotencyKey(UUID matchIdempotencyKey) { this.matchIdempotencyKey = matchIdempotencyKey; }
}
