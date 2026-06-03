package com.eprocure.inventory.domain.repository;

import com.eprocure.inventory.domain.model.GoodsReceiptStatus;
import java.time.Instant;
import java.util.UUID;

public record GoodsReceiptFilter(
        GoodsReceiptStatus status,
        UUID poId,
        UUID warehouseId,
        Instant fromReceivedAt,
        Instant toReceivedAtExclusive,
        int page,
        int size,
        int offset) {
}
