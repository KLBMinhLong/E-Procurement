package com.eprocure.finance.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record GoodsReceiptSnapshot(
        UUID id,
        String grNumber,
        UUID poId,
        String poNumber,
        UUID warehouseId,
        UUID warehouseKeeperId,
        GoodsReceiptSnapshotStatus status,
        Instant receivedAt,
        Instant completedAt,
        List<GoodsReceiptLineSnapshot> lineItems,
        String sourceEventId) {

    public GoodsReceiptSnapshot {
        id = Objects.requireNonNull(id, "id must not be null");
        grNumber = requireText(grNumber, "grNumber");
        poId = Objects.requireNonNull(poId, "poId must not be null");
        poNumber = requireText(poNumber, "poNumber");
        warehouseId = Objects.requireNonNull(warehouseId, "warehouseId must not be null");
        warehouseKeeperId = Objects.requireNonNull(warehouseKeeperId, "warehouseKeeperId must not be null");
        status = Objects.requireNonNull(status, "status must not be null");
        receivedAt = Objects.requireNonNull(receivedAt, "receivedAt must not be null");
        completedAt = Objects.requireNonNull(completedAt, "completedAt must not be null");
        lineItems = List.copyOf(lineItems == null ? List.of() : lineItems);
        if (lineItems.isEmpty()) {
            throw new IllegalArgumentException("lineItems must not be empty");
        }
        sourceEventId = requireText(sourceEventId, "sourceEventId");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
