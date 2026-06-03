package com.eprocure.inventory.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record GoodsReceipt(
        UUID id,
        String grNumber,
        UUID poId,
        String poNumber,
        UUID warehouseId,
        String warehouseName,
        UUID warehouseKeeperId,
        String warehouseKeeperFullName,
        Instant receivedAt,
        GoodsReceiptStatus status,
        List<GoodsReceiptLineItem> lineItems,
        String notes,
        Instant createdAt,
        UUID createdBy,
        UUID updatedBy,
        UUID idempotencyKey) {

    public GoodsReceipt {
        id = Objects.requireNonNull(id, "id must not be null");
        grNumber = requireText(grNumber, "grNumber");
        poId = Objects.requireNonNull(poId, "poId must not be null");
        poNumber = requireText(poNumber, "poNumber");
        warehouseId = Objects.requireNonNull(warehouseId, "warehouseId must not be null");
        warehouseName = requireText(warehouseName, "warehouseName");
        warehouseKeeperId = Objects.requireNonNull(warehouseKeeperId, "warehouseKeeperId must not be null");
        warehouseKeeperFullName = requireText(warehouseKeeperFullName, "warehouseKeeperFullName");
        receivedAt = Objects.requireNonNull(receivedAt, "receivedAt must not be null");
        status = Objects.requireNonNull(status, "status must not be null");
        lineItems = List.copyOf(Objects.requireNonNull(lineItems, "lineItems must not be null"));
        if (lineItems.isEmpty()) {
            throw new IllegalArgumentException("lineItems must not be empty");
        }
        notes = normalizeNullable(notes);
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        createdBy = Objects.requireNonNull(createdBy, "createdBy must not be null");
        idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
    }

    public static GoodsReceipt create(
            UUID id,
            String grNumber,
            UUID poId,
            String poNumber,
            UUID warehouseId,
            String warehouseName,
            UUID actorId,
            String actorFullName,
            Instant receivedAt,
            List<GoodsReceiptLineItem> lineItems,
            String notes,
            Instant createdAt,
            UUID idempotencyKey) {
        return new GoodsReceipt(
                id,
                grNumber,
                poId,
                poNumber,
                warehouseId,
                warehouseName,
                actorId,
                actorFullName,
                receivedAt == null ? createdAt : receivedAt,
                GoodsReceiptStatus.DRAFT,
                lineItems,
                notes,
                createdAt,
                actorId,
                null,
                idempotencyKey);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
