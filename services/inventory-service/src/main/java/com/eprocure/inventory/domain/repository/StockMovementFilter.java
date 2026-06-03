package com.eprocure.inventory.domain.repository;

import com.eprocure.inventory.domain.model.StockMovementType;
import java.time.Instant;
import java.util.UUID;

public record StockMovementFilter(
        String itemCode,
        UUID warehouseId,
        StockMovementType movementType,
        Instant fromPerformedAt,
        Instant toPerformedAtExclusive,
        int page,
        int size,
        int offset) {

    public StockMovementFilter {
        itemCode = normalizeNullable(itemCode);
        page = Math.max(page, 1);
        size = Math.max(1, Math.min(size, 100));
        offset = Math.max(offset, 0);
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
