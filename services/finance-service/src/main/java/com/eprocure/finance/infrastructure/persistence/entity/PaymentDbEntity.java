package com.eprocure.finance.infrastructure.persistence.entity;

import com.eprocure.finance.domain.model.Payment;
import com.eprocure.finance.domain.model.PaymentStatus;
import com.eprocure.finance.domain.model.vo.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class PaymentDbEntity {
    private UUID id;
    private UUID invoiceId;
    private LocalDate paymentDate;
    private String paymentReference;
    private BigDecimal paidAmount;
    private String currency;
    private String notes;
    private PaymentStatus status;
    private UUID idempotencyKey;
    private Instant confirmedAt;
    private UUID confirmedBy;

    public static PaymentDbEntity from(Payment payment) {
        PaymentDbEntity entity = new PaymentDbEntity();
        entity.id = payment.id();
        entity.invoiceId = payment.invoiceId();
        entity.paymentDate = payment.paymentDate();
        entity.paymentReference = payment.paymentReference();
        entity.paidAmount = payment.paidAmount().amount();
        entity.currency = payment.paidAmount().currency();
        entity.notes = payment.notes();
        entity.status = payment.status();
        entity.idempotencyKey = payment.idempotencyKey();
        entity.confirmedAt = payment.confirmedAt();
        entity.confirmedBy = payment.confirmedBy();
        return entity;
    }

    public Payment toDomain() {
        return new Payment(
                id,
                invoiceId,
                paymentDate,
                paymentReference,
                new Money(paidAmount, currency),
                notes,
                status,
                idempotencyKey,
                confirmedAt,
                confirmedBy);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getInvoiceId() { return invoiceId; }
    public void setInvoiceId(UUID invoiceId) { this.invoiceId = invoiceId; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public UUID getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(UUID idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(Instant confirmedAt) { this.confirmedAt = confirmedAt; }
    public UUID getConfirmedBy() { return confirmedBy; }
    public void setConfirmedBy(UUID confirmedBy) { this.confirmedBy = confirmedBy; }
}
