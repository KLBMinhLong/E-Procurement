package com.eprocure.finance.infrastructure.persistence.mapper;

import com.eprocure.finance.infrastructure.persistence.entity.PaymentDbEntity;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PaymentMapper {
    @Select("""
            SELECT id, invoice_id, payment_date, payment_reference, paid_amount,
                   currency, notes, status, idempotency_key, confirmed_at, confirmed_by
            FROM finance.payments
            WHERE idempotency_key = #{idempotencyKey}
              AND is_deleted = FALSE
            """)
    Optional<PaymentDbEntity> findByIdempotencyKey(@Param("idempotencyKey") UUID idempotencyKey);

    @Select("""
            SELECT id, invoice_id, payment_date, payment_reference, paid_amount,
                   currency, notes, status, idempotency_key, confirmed_at, confirmed_by
            FROM finance.payments
            WHERE invoice_id = #{invoiceId}
              AND is_deleted = FALSE
            ORDER BY confirmed_at DESC
            LIMIT 1
            """)
    Optional<PaymentDbEntity> findByInvoiceId(@Param("invoiceId") UUID invoiceId);

    @Insert("""
            INSERT INTO finance.payments (
                id, invoice_id, payment_date, payment_reference, paid_amount,
                currency, notes, status, idempotency_key, confirmed_at, confirmed_by,
                created_by
            ) VALUES (
                #{entity.id}, #{entity.invoiceId}, #{entity.paymentDate}, #{entity.paymentReference},
                #{entity.paidAmount}, #{entity.currency}, #{entity.notes}, #{entity.status},
                #{entity.idempotencyKey}, #{entity.confirmedAt}, #{entity.confirmedBy},
                #{entity.confirmedBy}
            )
            """)
    void insert(@Param("entity") PaymentDbEntity entity);
}
