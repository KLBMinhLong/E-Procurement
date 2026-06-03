package com.eprocure.finance.domain.repository;

import com.eprocure.finance.domain.model.Invoice;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository {
    Optional<Invoice> findById(UUID invoiceId);

    Optional<Invoice> findByIdempotencyKey(UUID idempotencyKey);

    Optional<Invoice> findByVendorIdAndInvoiceNumber(UUID vendorId, String invoiceNumber);

    List<Invoice> findByFilter(InvoiceFilter filter);

    long countByFilter(InvoiceFilter filter);

    void insert(Invoice invoice);
}
