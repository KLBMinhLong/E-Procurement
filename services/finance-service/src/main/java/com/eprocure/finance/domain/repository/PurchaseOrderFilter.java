package com.eprocure.finance.domain.repository;

import com.eprocure.finance.domain.model.PurchaseOrderStatus;
import java.time.Instant;
import java.util.UUID;

public record PurchaseOrderFilter(
        UUID purchasingOfficerId,
        PurchaseOrderStatus status,
        UUID vendorId,
        Instant fromCreatedAt,
        Instant toCreatedAtExclusive,
        int page,
        int size,
        int offset,
        String sortField,
        String sortDirection) {
}
