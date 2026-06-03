package com.eprocure.inventory.domain.repository;

import java.util.UUID;

public record StockEntryFilter(
        String itemCode,
        UUID warehouseId,
        Boolean belowReorder,
        int page,
        int size,
        int offset) {

    public StockEntryFilter {
        itemCode = normalizeNullable(itemCode);
        page = Math.max(page, 1);
        size = Math.max(1, Math.min(size, 1000));
        offset = Math.max(offset, 0);
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
