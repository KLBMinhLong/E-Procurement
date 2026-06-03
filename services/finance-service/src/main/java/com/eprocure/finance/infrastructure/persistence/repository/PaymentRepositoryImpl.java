package com.eprocure.finance.infrastructure.persistence.repository;

import com.eprocure.finance.domain.model.Payment;
import com.eprocure.finance.domain.repository.PaymentRepository;
import com.eprocure.finance.infrastructure.persistence.entity.PaymentDbEntity;
import com.eprocure.finance.infrastructure.persistence.mapper.PaymentMapper;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentRepositoryImpl implements PaymentRepository {
    private final PaymentMapper paymentMapper;

    public PaymentRepositoryImpl(PaymentMapper paymentMapper) {
        this.paymentMapper = paymentMapper;
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(UUID idempotencyKey) {
        return paymentMapper.findByIdempotencyKey(idempotencyKey).map(PaymentDbEntity::toDomain);
    }

    @Override
    public Optional<Payment> findByInvoiceId(UUID invoiceId) {
        return paymentMapper.findByInvoiceId(invoiceId).map(PaymentDbEntity::toDomain);
    }

    @Override
    public void insert(Payment payment) {
        paymentMapper.insert(PaymentDbEntity.from(payment));
    }
}
