package com.eprocure.finance.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record ConfirmPaymentCommand(
        UUID actorId,
        UUID invoiceId,
        LocalDate paymentDate,
        String paymentReference,
        BigDecimal paidAmount,
        String notes) {

    public ConfirmPaymentCommand {
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        invoiceId = Objects.requireNonNull(invoiceId, "invoiceId must not be null");
        paymentDate = Objects.requireNonNull(paymentDate, "paymentDate must not be null");
        paymentReference = requireText(paymentReference, "paymentReference");
        if (paidAmount != null && paidAmount.signum() <= 0) {
            throw new IllegalArgumentException("paidAmount must be positive");
        }
        notes = notes == null || notes.isBlank() ? null : notes.trim();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
