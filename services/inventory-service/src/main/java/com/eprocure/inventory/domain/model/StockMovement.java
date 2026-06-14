package com.eprocure.inventory.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record StockMovement(
        UUID id,
        String itemCode,
        UUID warehouseId,
        StockMovementType movementType,
        BigDecimal quantity,
        String unit,
        BigDecimal balanceAfter,
        String sourceRefType,
        UUID sourceRefId,
        UUID performedBy,
        Instant performedAt,
        String notes) {

    public StockMovement {
        id = Objects.requireNonNull(id, "id must not be null");
        itemCode = requireText(itemCode, "itemCode");
        warehouseId = Objects.requireNonNull(warehouseId, "warehouseId must not be null");
        movementType = Objects.requireNonNull(movementType, "movementType must not be null");
        quantity = requireNonZero(quantity, "quantity");
        unit = requireText(unit, "unit");
        balanceAfter = requireNonNegative(balanceAfter, "balanceAfter");
        sourceRefType = normalizeNullable(sourceRefType);
        sourceRefId = Objects.requireNonNull(sourceRefId, "sourceRefId must not be null");
        performedBy = Objects.requireNonNull(performedBy, "performedBy must not be null");
        performedAt = Objects.requireNonNull(performedAt, "performedAt must not be null");
        notes = normalizeNullable(notes);
    }

    public static StockMovement receiptIn(
            String itemCode,
            UUID warehouseId,
            BigDecimal quantity,
            String unit,
            BigDecimal balanceAfter,
            UUID goodsReceiptId,
            UUID actorId,
            Instant performedAt,
            String notes) {
        return new StockMovement(
                UUID.randomUUID(),
                itemCode,
                warehouseId,
                StockMovementType.RECEIPT_IN,
                quantity,
                unit,
                balanceAfter,
                "GOODS_RECEIPT",
                goodsReceiptId,
                actorId,
                performedAt,
                notes);
    }

    public static StockMovement issueOut(
            String itemCode,
            UUID warehouseId,
            BigDecimal quantity,
            String unit,
            BigDecimal balanceAfter,
            UUID issueOutRequestId,
            UUID actorId,
            Instant performedAt,
            String notes) {
        BigDecimal checkedQuantity = requirePositive(quantity, "quantity");
        return new StockMovement(
                UUID.randomUUID(),
                itemCode,
                warehouseId,
                StockMovementType.ISSUE_OUT,
                checkedQuantity.negate(),
                unit,
                balanceAfter,
                "STOCK_ISSUE_OUT",
                issueOutRequestId,
                actorId,
                performedAt,
                notes);
    }

    public static StockMovement adjustment(
            String itemCode,
            UUID warehouseId,
            BigDecimal delta,
            String unit,
            BigDecimal balanceAfter,
            UUID adjustmentRequestId,
            UUID actorId,
            Instant performedAt,
            String notes) {
        return new StockMovement(
                UUID.randomUUID(),
                itemCode,
                warehouseId,
                StockMovementType.ADJUSTMENT,
                delta,
                unit,
                balanceAfter,
                "STOCK_ADJUSTMENT",
                adjustmentRequestId,
                actorId,
                performedAt,
                notes);
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

    private static BigDecimal requireNonZero(BigDecimal value, String fieldName) {
        BigDecimal checked = Objects.requireNonNull(value, fieldName + " must not be null");
        if (checked.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException(fieldName + " must not be zero");
        }
        return checked;
    }

    private static BigDecimal requirePositive(BigDecimal value, String fieldName) {
        BigDecimal checked = Objects.requireNonNull(value, fieldName + " must not be null");
        if (checked.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero");
        }
        return checked;
    }

    private static BigDecimal requireNonNegative(BigDecimal value, String fieldName) {
        BigDecimal checked = Objects.requireNonNull(value, fieldName + " must not be null");
        if (checked.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
        return checked;
    }
}
