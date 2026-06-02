package com.eprocure.vendor.infrastructure.persistence.entity;

import com.eprocure.vendor.domain.model.VendorQuote;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class VendorQuoteDbEntity {
    private UUID id;
    private UUID rfqId;
    private UUID vendorId;
    private String vendorName;
    private List<VendorQuoteLineItemDbEntity> lineItems = List.of();
    private BigDecimal totalAmount;
    private String currency;
    private LocalDate validUntil;
    private String paymentTerms;
    private String notes;
    private Instant submittedAt;
    private BigDecimal evaluationScore;
    private String evaluationNote;
    private UUID evaluatedBy;
    private Instant evaluatedAt;
    private UUID idempotencyKey;
    private Instant createdAt;
    private UUID createdBy;
    private UUID updatedBy;

    public static VendorQuoteDbEntity from(VendorQuote quote) {
        VendorQuoteDbEntity entity = new VendorQuoteDbEntity();
        entity.id = quote.id();
        entity.rfqId = quote.rfqId();
        entity.vendorId = quote.vendorId();
        entity.vendorName = quote.vendorName();
        entity.totalAmount = quote.totalAmount();
        entity.currency = quote.currency();
        entity.validUntil = quote.validUntil();
        entity.paymentTerms = quote.paymentTerms();
        entity.notes = quote.notes();
        entity.submittedAt = quote.submittedAt();
        entity.evaluationScore = quote.evaluationScore();
        entity.evaluationNote = quote.evaluationNote();
        entity.evaluatedBy = quote.evaluatedBy();
        entity.evaluatedAt = quote.evaluatedAt();
        entity.idempotencyKey = quote.idempotencyKey();
        entity.createdAt = quote.createdAt();
        entity.createdBy = quote.createdBy();
        entity.updatedBy = quote.updatedBy();
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

    public UUID getVendorId() {
        return vendorId;
    }

    public void setVendorId(UUID vendorId) {
        this.vendorId = vendorId;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public List<VendorQuoteLineItemDbEntity> getLineItems() {
        return lineItems;
    }

    public void setLineItems(List<VendorQuoteLineItemDbEntity> lineItems) {
        this.lineItems = List.copyOf(lineItems == null ? List.of() : lineItems);
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDate validUntil) {
        this.validUntil = validUntil;
    }

    public String getPaymentTerms() {
        return paymentTerms;
    }

    public void setPaymentTerms(String paymentTerms) {
        this.paymentTerms = paymentTerms;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public BigDecimal getEvaluationScore() {
        return evaluationScore;
    }

    public void setEvaluationScore(BigDecimal evaluationScore) {
        this.evaluationScore = evaluationScore;
    }

    public String getEvaluationNote() {
        return evaluationNote;
    }

    public void setEvaluationNote(String evaluationNote) {
        this.evaluationNote = evaluationNote;
    }

    public UUID getEvaluatedBy() {
        return evaluatedBy;
    }

    public void setEvaluatedBy(UUID evaluatedBy) {
        this.evaluatedBy = evaluatedBy;
    }

    public Instant getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(Instant evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }

    public UUID getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(UUID idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }
}
