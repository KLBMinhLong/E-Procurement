package com.eprocure.finance.domain.model;

import com.eprocure.finance.domain.model.vo.Money;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record Payment(
        UUID id,
        UUID invoiceId,
        LocalDate paymentDate,
        String paymentReference,
        Money paidAmount,
        String notes,
        PaymentStatus status,
        UUID idempotencyKey,
        Instant confirmedAt,
        UUID confirmedBy) {

    public Payment {
        id = Objects.requireNonNull(id, "id must not be null");
        invoiceId = Objects.requireNonNull(invoiceId, "invoiceId must not be null");
        paymentDate = Objects.requireNonNull(paymentDate, "paymentDate must not be null");
        paymentReference = requireText(paymentReference, "paymentReference");
        paidAmount = Objects.requireNonNull(paidAmount, "paidAmount must not be null");
        if (!paidAmount.isPositive()) {
            throw new IllegalArgumentException("paidAmount must be positive");
        }
        notes = notes == null || notes.isBlank() ? null : notes.trim();
        status = Objects.requireNonNull(status, "status must not be null");
        idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        confirmedAt = Objects.requireNonNull(confirmedAt, "confirmedAt must not be null");
        confirmedBy = Objects.requireNonNull(confirmedBy, "confirmedBy must not be null");
    }

    public static Payment confirm(
            UUID invoiceId,
            LocalDate paymentDate,
            String paymentReference,
            Money paidAmount,
            String notes,
            UUID idempotencyKey,
            Instant confirmedAt,
            UUID confirmedBy) {
        return new Payment(
                UUID.randomUUID(),
                invoiceId,
                paymentDate,
                paymentReference,
                paidAmount,
                notes,
                PaymentStatus.CONFIRMED,
                idempotencyKey,
                confirmedAt,
                confirmedBy);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
