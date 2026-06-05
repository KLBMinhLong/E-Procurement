package com.eprocure.analytics.domain.model.projection;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record GoodsReceiptCreatedProjection(
        AnalyticsEventMetadata eventMetadata,
        UUID grId,
        String grNumber,
        UUID poId,
        String poNumber,
        UUID warehouseId,
        UUID warehouseKeeperId,
        String status,
        Instant receivedAt,
        Instant completedAt,
        List<GoodsReceiptLineProjection> lineItems) {

    public GoodsReceiptCreatedProjection {
        eventMetadata = Objects.requireNonNull(eventMetadata, "eventMetadata must not be null");
        grId = Objects.requireNonNull(grId, "grId must not be null");
        grNumber = requireText(grNumber, "grNumber");
        poId = Objects.requireNonNull(poId, "poId must not be null");
        poNumber = requireText(poNumber, "poNumber");
        warehouseId = Objects.requireNonNull(warehouseId, "warehouseId must not be null");
        status = requireText(status, "status").toUpperCase();
        receivedAt = receivedAt == null ? eventMetadata.eventTimestamp() : receivedAt;
        completedAt = completedAt == null ? eventMetadata.eventTimestamp() : completedAt;
        lineItems = List.copyOf(lineItems == null ? List.of() : lineItems);
        if (lineItems.isEmpty()) {
            throw new IllegalArgumentException("lineItems must not be empty");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
