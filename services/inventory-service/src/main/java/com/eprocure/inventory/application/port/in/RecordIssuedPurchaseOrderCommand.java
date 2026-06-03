package com.eprocure.inventory.application.port.in;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record RecordIssuedPurchaseOrderCommand(
        String eventId,
        String topic,
        Integer partitionId,
        Long offsetValue,
        Instant occurredAt,
        UUID poId,
        String poNumber,
        UUID prId,
        String prNumber,
        UUID vendorId,
        String vendorName,
        String vendorEmail,
        String vendorTaxCode,
        UUID purchasingOfficerId,
        BigDecimal totalAmount,
        String currency,
        String deliveryAddress,
        LocalDate deliveryDeadline,
        String paymentTerms,
        Instant issuedAt,
        Instant sentToVendorAt,
        List<LineItem> lineItems) {

    public RecordIssuedPurchaseOrderCommand {
        eventId = requireText(eventId, "eventId");
        topic = requireText(topic, "topic");
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        poId = Objects.requireNonNull(poId, "poId must not be null");
        poNumber = requireText(poNumber, "poNumber");
        vendorId = Objects.requireNonNull(vendorId, "vendorId must not be null");
        vendorName = requireText(vendorName, "vendorName");
        purchasingOfficerId = Objects.requireNonNull(purchasingOfficerId, "purchasingOfficerId must not be null");
        totalAmount = requireNonNegative(totalAmount, "totalAmount");
        currency = requireText(currency, "currency");
        issuedAt = Objects.requireNonNull(issuedAt, "issuedAt must not be null");
        lineItems = List.copyOf(Objects.requireNonNull(lineItems, "lineItems must not be null"));
        if (lineItems.isEmpty()) {
            throw new IllegalArgumentException("lineItems must not be empty");
        }
    }

    public record LineItem(
            UUID poLineItemId,
            UUID prLineItemId,
            String itemName,
            String categoryCode,
            BigDecimal quantity,
            String unit,
            BigDecimal unitPrice,
            BigDecimal totalPrice,
            String currency) {

        public LineItem {
            poLineItemId = Objects.requireNonNull(poLineItemId, "poLineItemId must not be null");
            itemName = requireText(itemName, "itemName");
            categoryCode = requireText(categoryCode, "categoryCode");
            quantity = requirePositive(quantity, "quantity");
            unit = requireText(unit, "unit");
            unitPrice = requireNonNegative(unitPrice, "unitPrice");
            totalPrice = requireNonNegative(totalPrice, "totalPrice");
            currency = requireText(currency, "currency");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
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
