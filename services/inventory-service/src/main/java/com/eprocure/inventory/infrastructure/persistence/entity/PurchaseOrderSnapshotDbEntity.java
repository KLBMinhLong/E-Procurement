package com.eprocure.inventory.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class PurchaseOrderSnapshotDbEntity {
    private UUID id;
    private UUID poId;
    private String poNumber;
    private UUID prId;
    private String prNumber;
    private UUID vendorId;
    private String vendorName;
    private String vendorEmail;
    private String vendorTaxCode;
    private UUID purchasingOfficerId;
    private BigDecimal totalAmount;
    private String currency;
    private String deliveryAddress;
    private LocalDate deliveryDeadline;
    private String paymentTerms;
    private Instant issuedAt;
    private Instant sentToVendorAt;
    private Instant createdAt;
    private String sourceEventId;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPoId() {
        return poId;
    }

    public void setPoId(UUID poId) {
        this.poId = poId;
    }

    public String getPoNumber() {
        return poNumber;
    }

    public void setPoNumber(String poNumber) {
        this.poNumber = poNumber;
    }

    public UUID getPrId() {
        return prId;
    }

    public void setPrId(UUID prId) {
        this.prId = prId;
    }

    public String getPrNumber() {
        return prNumber;
    }

    public void setPrNumber(String prNumber) {
        this.prNumber = prNumber;
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

    public String getVendorEmail() {
        return vendorEmail;
    }

    public void setVendorEmail(String vendorEmail) {
        this.vendorEmail = vendorEmail;
    }

    public String getVendorTaxCode() {
        return vendorTaxCode;
    }

    public void setVendorTaxCode(String vendorTaxCode) {
        this.vendorTaxCode = vendorTaxCode;
    }

    public UUID getPurchasingOfficerId() {
        return purchasingOfficerId;
    }

    public void setPurchasingOfficerId(UUID purchasingOfficerId) {
        this.purchasingOfficerId = purchasingOfficerId;
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

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public LocalDate getDeliveryDeadline() {
        return deliveryDeadline;
    }

    public void setDeliveryDeadline(LocalDate deliveryDeadline) {
        this.deliveryDeadline = deliveryDeadline;
    }

    public String getPaymentTerms() {
        return paymentTerms;
    }

    public void setPaymentTerms(String paymentTerms) {
        this.paymentTerms = paymentTerms;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Instant getSentToVendorAt() {
        return sentToVendorAt;
    }

    public void setSentToVendorAt(Instant sentToVendorAt) {
        this.sentToVendorAt = sentToVendorAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getSourceEventId() {
        return sourceEventId;
    }

    public void setSourceEventId(String sourceEventId) {
        this.sourceEventId = sourceEventId;
    }
}
