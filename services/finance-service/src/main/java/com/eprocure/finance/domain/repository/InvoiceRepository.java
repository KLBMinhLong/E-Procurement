package com.eprocure.finance.domain.repository;

import com.eprocure.finance.domain.model.Invoice;
import com.eprocure.finance.domain.model.InvoiceStatus;
import com.eprocure.finance.domain.model.MatchStatus;
import com.eprocure.finance.domain.model.vo.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository {
    Optional<Invoice> findById(UUID invoiceId);

    Optional<Invoice> findByIdempotencyKey(UUID idempotencyKey);

    Optional<Invoice> findByIdAndMatchIdempotencyKey(UUID invoiceId, UUID idempotencyKey);

    Optional<Invoice> findByIdAndApprovalIdempotencyKey(UUID invoiceId, UUID idempotencyKey);

    Optional<Invoice> findByIdAndDisputeIdempotencyKey(UUID invoiceId, UUID idempotencyKey);

    Optional<Invoice> findByVendorIdAndInvoiceNumber(UUID vendorId, String invoiceNumber);

    List<Invoice> findByFilter(InvoiceFilter filter);

    long countByFilter(InvoiceFilter filter);

    void insert(Invoice invoice);

    void updateMatchResult(
            UUID invoiceId,
            InvoiceStatus status,
            MatchStatus poMatchStatus,
            MatchStatus grMatchStatus,
            BigDecimal qtyVariance,
            Money priceVariance,
            Instant matchedAt,
            UUID matchedBy,
            UUID idempotencyKey);

    void markApproved(UUID invoiceId, UUID approvedBy, Instant approvedAt, UUID idempotencyKey);

    void markDisputed(UUID invoiceId, String reason, UUID disputedBy, Instant disputedAt, UUID idempotencyKey);

    void markPaid(UUID invoiceId, UUID paidBy, Instant paidAt);
}
