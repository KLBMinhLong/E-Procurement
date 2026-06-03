package com.eprocure.inventory.application.port.in;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record CreateGoodsReceiptCommand(
        UUID actorId,
        String actorFullName,
        UUID poId,
        UUID warehouseId,
        Instant receivedAt,
        List<LineItem> lineItems,
        String notes) {

    public CreateGoodsReceiptCommand {
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        actorFullName = actorFullName == null || actorFullName.isBlank() ? actorId.toString() : actorFullName.trim();
        poId = Objects.requireNonNull(poId, "poId must not be null");
        warehouseId = Objects.requireNonNull(warehouseId, "warehouseId must not be null");
        lineItems = List.copyOf(Objects.requireNonNull(lineItems, "lineItems must not be null"));
        if (lineItems.isEmpty()) {
            throw new IllegalArgumentException("lineItems must not be empty");
        }
        notes = notes == null || notes.isBlank() ? null : notes.trim();
    }

    public record LineItem(
            UUID poLineItemId,
            BigDecimal receivedQuantity,
            BigDecimal rejectedQuantity,
            String rejectionReason,
            String lotNumber) {

        public LineItem {
            poLineItemId = Objects.requireNonNull(poLineItemId, "poLineItemId must not be null");
            receivedQuantity = requireNonNegative(receivedQuantity, "receivedQuantity");
            rejectedQuantity = rejectedQuantity == null ? BigDecimal.ZERO : requireNonNegative(rejectedQuantity, "rejectedQuantity");
            rejectionReason = rejectionReason == null || rejectionReason.isBlank() ? null : rejectionReason.trim();
            lotNumber = lotNumber == null || lotNumber.isBlank() ? null : lotNumber.trim();
        }

        public BigDecimal inspectedQuantity() {
            return receivedQuantity.add(rejectedQuantity);
        }
    }

    private static BigDecimal requireNonNegative(BigDecimal value, String fieldName) {
        BigDecimal checked = Objects.requireNonNull(value, fieldName + " must not be null");
        if (checked.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
        return checked;
    }
}
