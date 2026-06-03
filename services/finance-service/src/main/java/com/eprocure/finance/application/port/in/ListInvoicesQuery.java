package com.eprocure.finance.application.port.in;

import com.eprocure.finance.domain.model.InvoiceStatus;
import java.util.UUID;

public record ListInvoicesQuery(
        UUID actorId,
        InvoiceStatus status,
        UUID vendorId,
        UUID poId,
        Boolean overdueOnly,
        int page,
        int size) {

    public ListInvoicesQuery {
        actorId = java.util.Objects.requireNonNull(actorId, "actorId must not be null");
        page = Math.max(page, 1);
        size = Math.max(1, Math.min(size, 100));
    }
}
