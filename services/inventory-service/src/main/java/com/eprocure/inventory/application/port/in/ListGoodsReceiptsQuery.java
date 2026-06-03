package com.eprocure.inventory.application.port.in;

import com.eprocure.inventory.domain.model.GoodsReceiptStatus;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record ListGoodsReceiptsQuery(
        UUID actorId,
        GoodsReceiptStatus status,
        UUID poId,
        UUID warehouseId,
        LocalDate fromDate,
        LocalDate toDate,
        int page,
        int size) {

    public ListGoodsReceiptsQuery {
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        page = Math.max(page, 1);
        size = Math.max(1, Math.min(size, 100));
    }
}
