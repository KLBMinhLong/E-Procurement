package com.eprocure.inventory.application.port.in;

import com.eprocure.inventory.domain.model.StockMovementType;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record ListStockMovementsQuery(
        UUID actorId,
        String itemCode,
        UUID warehouseId,
        StockMovementType movementType,
        LocalDate fromDate,
        LocalDate toDate,
        int page,
        int size) {

    public ListStockMovementsQuery {
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        itemCode = normalizeNullable(itemCode);
        page = Math.max(page, 1);
        size = Math.max(1, Math.min(size, 100));
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
