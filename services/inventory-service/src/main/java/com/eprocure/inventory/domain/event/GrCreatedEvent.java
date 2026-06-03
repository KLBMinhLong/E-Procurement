package com.eprocure.inventory.domain.event;

import com.eprocure.inventory.domain.model.GoodsReceipt;
import com.eprocure.inventory.domain.model.GoodsReceiptLineItem;
import com.eprocure.inventory.domain.model.GoodsReceiptStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record GrCreatedEvent(
        String eventId,
        String eventType,
        String version,
        String source,
        Instant timestamp,
        UUID traceId,
        Payload payload) {

    public static GrCreatedEvent create(
            GoodsReceipt goodsReceipt,
            GoodsReceiptStatus status,
            Map<UUID, String> resolvedItemCodes,
            Instant completedAt) {
        Objects.requireNonNull(goodsReceipt, "goodsReceipt must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(resolvedItemCodes, "resolvedItemCodes must not be null");
        Objects.requireNonNull(completedAt, "completedAt must not be null");
        return new GrCreatedEvent(
                UUID.randomUUID().toString(),
                "GR_CREATED",
                "1.0",
                "inventory-service",
                completedAt,
                UUID.randomUUID(),
                Payload.from(goodsReceipt, status, resolvedItemCodes, completedAt));
    }

    public GrCreatedEvent {
        eventId = requireText(eventId, "eventId");
        eventType = requireText(eventType, "eventType");
        version = requireText(version, "version");
        source = requireText(source, "source");
        timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
        traceId = Objects.requireNonNull(traceId, "traceId must not be null");
        payload = Objects.requireNonNull(payload, "payload must not be null");
    }

    public record Payload(
            UUID grId,
            String grNumber,
            UUID poId,
            String poNumber,
            UUID warehouseId,
            UUID warehouseKeeperId,
            GoodsReceiptStatus status,
            Instant receivedAt,
            Instant completedAt,
            List<LineItem> lineItems) {

        public static Payload from(
                GoodsReceipt goodsReceipt,
                GoodsReceiptStatus status,
                Map<UUID, String> resolvedItemCodes,
                Instant completedAt) {
            return new Payload(
                    goodsReceipt.id(),
                    goodsReceipt.grNumber(),
                    goodsReceipt.poId(),
                    goodsReceipt.poNumber(),
                    goodsReceipt.warehouseId(),
                    goodsReceipt.warehouseKeeperId(),
                    status,
                    goodsReceipt.receivedAt(),
                    completedAt,
                    goodsReceipt.lineItems().stream()
                            .map(lineItem -> LineItem.from(lineItem, resolvedItemCodes.get(lineItem.id())))
                            .toList());
        }
    }

    public record LineItem(
            UUID grLineItemId,
            UUID poLineItemId,
            String itemCode,
            String itemName,
            BigDecimal orderedQuantity,
            BigDecimal receivedQuantity,
            BigDecimal rejectedQuantity,
            String unit) {

        public static LineItem from(GoodsReceiptLineItem lineItem, String resolvedItemCode) {
            return new LineItem(
                    lineItem.id(),
                    lineItem.poLineItemId(),
                    resolvedItemCode == null ? lineItem.itemCode() : resolvedItemCode,
                    lineItem.itemName(),
                    lineItem.orderedQuantity(),
                    lineItem.receivedQuantity(),
                    lineItem.rejectedQuantity(),
                    lineItem.unit());
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
