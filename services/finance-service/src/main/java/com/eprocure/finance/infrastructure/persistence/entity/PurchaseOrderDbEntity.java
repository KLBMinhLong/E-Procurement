package com.eprocure.finance.infrastructure.persistence.entity;

import com.eprocure.finance.domain.model.PurchaseOrder;
import com.eprocure.finance.domain.model.PurchaseOrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class PurchaseOrderDbEntity {
    private UUID id;
    private String poNumber;
    private UUID prId;
    private String prNumber;
    private UUID rfqId;
    private String rfqNumber;
    private UUID awardedQuoteId;
    private UUID vendorId;
    private String vendorName;
    private String vendorEmail;
    private String vendorTaxCode;
    private UUID purchasingOfficerId;
    private String purchasingOfficerFullName;
    private PurchaseOrderStatus status;
    private BigDecimal totalAmount;
    private String currency;
    private String deliveryAddress;
    private LocalDate deliveryDeadline;
    private String paymentTerms;
    private String vendorNote;
    private Instant issuedAt;
    private Instant sentToVendorAt;
    private Instant cancelledAt;
    private UUID cancelledBy;
    private String cancelReason;
    private Instant createdAt;
    private String sourceEventId;

    public static PurchaseOrderDbEntity from(PurchaseOrder purchaseOrder) {
        PurchaseOrderDbEntity entity = new PurchaseOrderDbEntity();
        entity.id = purchaseOrder.id();
        entity.poNumber = purchaseOrder.poNumber();
        entity.prId = purchaseOrder.prId();
        entity.prNumber = purchaseOrder.prNumber();
        entity.rfqId = purchaseOrder.rfqId();
        entity.rfqNumber = purchaseOrder.rfqNumber();
        entity.awardedQuoteId = purchaseOrder.awardedQuoteId();
        entity.vendorId = purchaseOrder.vendorId();
        entity.vendorName = purchaseOrder.vendorName();
        entity.vendorEmail = purchaseOrder.vendorEmail();
        entity.vendorTaxCode = purchaseOrder.vendorTaxCode();
        entity.purchasingOfficerId = purchaseOrder.purchasingOfficerId();
        entity.purchasingOfficerFullName = purchaseOrder.purchasingOfficerFullName();
        entity.status = purchaseOrder.status();
        entity.totalAmount = purchaseOrder.totalAmount().amount();
        entity.currency = purchaseOrder.totalAmount().currency();
        entity.deliveryAddress = purchaseOrder.deliveryAddress();
        entity.deliveryDeadline = purchaseOrder.deliveryDeadline();
        entity.paymentTerms = purchaseOrder.paymentTerms();
        entity.vendorNote = purchaseOrder.vendorNote();
        entity.issuedAt = purchaseOrder.issuedAt();
        entity.sentToVendorAt = purchaseOrder.sentToVendorAt();
        entity.cancelledAt = purchaseOrder.cancelledAt();
        entity.cancelledBy = purchaseOrder.cancelledBy();
        entity.cancelReason = purchaseOrder.cancelReason();
        entity.createdAt = purchaseOrder.createdAt();
        entity.sourceEventId = purchaseOrder.sourceEventId();
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public String getPoNumber() {
        return poNumber;
    }

    public UUID getPrId() {
        return prId;
    }

    public String getPrNumber() {
        return prNumber;
    }

    public UUID getRfqId() {
        return rfqId;
    }

    public String getRfqNumber() {
        return rfqNumber;
    }

    public UUID getAwardedQuoteId() {
        return awardedQuoteId;
    }

    public UUID getVendorId() {
        return vendorId;
    }

    public String getVendorName() {
        return vendorName;
    }

    public String getVendorEmail() {
        return vendorEmail;
    }

    public String getVendorTaxCode() {
        return vendorTaxCode;
    }

    public UUID getPurchasingOfficerId() {
        return purchasingOfficerId;
    }

    public String getPurchasingOfficerFullName() {
        return purchasingOfficerFullName;
    }

    public PurchaseOrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public LocalDate getDeliveryDeadline() {
        return deliveryDeadline;
    }

    public String getPaymentTerms() {
        return paymentTerms;
    }

    public String getVendorNote() {
        return vendorNote;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getSentToVendorAt() {
        return sentToVendorAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public UUID getCancelledBy() {
        return cancelledBy;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getSourceEventId() {
        return sourceEventId;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setPoNumber(String poNumber) {
        this.poNumber = poNumber;
    }

    public void setPrId(UUID prId) {
        this.prId = prId;
    }

    public void setPrNumber(String prNumber) {
        this.prNumber = prNumber;
    }

    public void setRfqId(UUID rfqId) {
        this.rfqId = rfqId;
    }

    public void setRfqNumber(String rfqNumber) {
        this.rfqNumber = rfqNumber;
    }

    public void setAwardedQuoteId(UUID awardedQuoteId) {
        this.awardedQuoteId = awardedQuoteId;
    }

    public void setVendorId(UUID vendorId) {
        this.vendorId = vendorId;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public void setVendorEmail(String vendorEmail) {
        this.vendorEmail = vendorEmail;
    }

    public void setVendorTaxCode(String vendorTaxCode) {
        this.vendorTaxCode = vendorTaxCode;
    }

    public void setPurchasingOfficerId(UUID purchasingOfficerId) {
        this.purchasingOfficerId = purchasingOfficerId;
    }

    public void setPurchasingOfficerFullName(String purchasingOfficerFullName) {
        this.purchasingOfficerFullName = purchasingOfficerFullName;
    }

    public void setStatus(PurchaseOrderStatus status) {
        this.status = status;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public void setDeliveryDeadline(LocalDate deliveryDeadline) {
        this.deliveryDeadline = deliveryDeadline;
    }

    public void setPaymentTerms(String paymentTerms) {
        this.paymentTerms = paymentTerms;
    }

    public void setVendorNote(String vendorNote) {
        this.vendorNote = vendorNote;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }

    public void setSentToVendorAt(Instant sentToVendorAt) {
        this.sentToVendorAt = sentToVendorAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public void setCancelledBy(UUID cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setSourceEventId(String sourceEventId) {
        this.sourceEventId = sourceEventId;
    }
}
