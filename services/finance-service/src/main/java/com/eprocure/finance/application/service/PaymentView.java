package com.eprocure.finance.application.service;

import com.eprocure.finance.domain.model.Payment;
import com.eprocure.finance.domain.model.PaymentStatus;
import com.eprocure.finance.domain.model.vo.Money;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PaymentView(
        UUID id,
        UUID invoiceId,
        LocalDate paymentDate,
        String paymentReference,
        Money paidAmount,
        String notes,
        PaymentStatus status,
        Instant confirmedAt,
        UUID confirmedBy) {

    public static PaymentView from(Payment payment) {
        return new PaymentView(
                payment.id(),
                payment.invoiceId(),
                payment.paymentDate(),
                payment.paymentReference(),
                payment.paidAmount(),
                payment.notes(),
                payment.status(),
                payment.confirmedAt(),
                payment.confirmedBy());
    }
}
