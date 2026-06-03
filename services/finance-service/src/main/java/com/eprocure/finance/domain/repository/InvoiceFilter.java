package com.eprocure.finance.domain.repository;

import com.eprocure.finance.domain.model.InvoiceStatus;
import java.time.LocalDate;
import java.util.UUID;

public record InvoiceFilter(
        InvoiceStatus status,
        UUID vendorId,
        UUID poId,
        Boolean overdueOnly,
        LocalDate overdueAsOf,
        int page,
        int size,
        int offset) {

    public InvoiceFilter {
        page = Math.max(page, 1);
        size = Math.max(1, Math.min(size, 100));
        offset = Math.max(offset, 0);
    }
}
