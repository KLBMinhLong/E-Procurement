package com.eprocure.inventory.application.port.in;

import java.util.Objects;
import java.util.UUID;

public record SearchItemsQuery(
        UUID actorId,
        String query,
        String categoryCode,
        Boolean active,
        Boolean belowReorder,
        int page,
        int size) {

    public SearchItemsQuery {
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        query = normalizeNullable(query);
        categoryCode = normalizeNullable(categoryCode);
        page = Math.max(page, 1);
        size = Math.max(1, Math.min(size, 100));
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
