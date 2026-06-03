package com.eprocure.inventory.application.port.in;

import java.util.Objects;
import java.util.UUID;

public record ListWarehouseStockQuery(
        UUID actorId,
        UUID warehouseId,
        Boolean belowReorder,
        int page,
        int size) {

    public ListWarehouseStockQuery {
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        warehouseId = Objects.requireNonNull(warehouseId, "warehouseId must not be null");
        page = Math.max(page, 1);
        size = Math.max(1, Math.min(size, 100));
    }
}
