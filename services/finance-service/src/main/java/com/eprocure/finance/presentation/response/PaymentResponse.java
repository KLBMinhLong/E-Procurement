package com.eprocure.finance.presentation.response;

import com.eprocure.finance.domain.model.PaymentStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID invoiceId,
        LocalDate paymentDate,
        String paymentReference,
        String paidAmount,
        String currency,
        String notes,
        PaymentStatus status,
        Instant confirmedAt,
        UUID confirmedBy) {
}
