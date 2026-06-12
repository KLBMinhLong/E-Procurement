package com.eprocure.inventory.domain.repository;

public record ItemFilter(
        String query,
        String categoryCode,
        Boolean active,
        Boolean belowReorder,
        int page,
        int size,
        int offset) {

    public ItemFilter {
        query = normalizeNullable(query);
        categoryCode = normalizeNullable(categoryCode);
        if (categoryCode != null) {
            categoryCode = categoryCode.toUpperCase();
        }
        page = Math.max(page, 1);
        size = Math.max(1, Math.min(size, 100));
        offset = Math.max(offset, 0);
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
