package com.eprocure.finance.domain.repository;

import com.eprocure.finance.domain.model.Payment;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {
    Optional<Payment> findByIdempotencyKey(UUID idempotencyKey);

    Optional<Payment> findByInvoiceId(UUID invoiceId);

    void insert(Payment payment);
}
