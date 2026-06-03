package com.eprocure.finance.application.port.in;

import com.eprocure.finance.domain.model.vo.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record CreatePurchaseOrderFromRfqAwardCommand(
        String eventId,
        String topic,
        Integer partitionId,
        Long offsetValue,
        UUID rfqId,
        String rfqNumber,
        UUID prId,
        String prNumber,
        UUID vendorId,
        String vendorName,
        String vendorEmail,
        String vendorTaxCode,
        UUID awardedQuoteId,
        Money totalAmount,
        String paymentTerms,
        UUID awardedBy,
        Instant occurredAt,
        List<LineItem> lineItems) {

    public CreatePurchaseOrderFromRfqAwardCommand {
        eventId = requireText(eventId, "eventId");
        topic = requireText(topic, "topic");
        rfqId = Objects.requireNonNull(rfqId, "rfqId must not be null");
        rfqNumber = requireText(rfqNumber, "rfqNumber");
        prId = Objects.requireNonNull(prId, "prId must not be null");
        prNumber = requireText(prNumber, "prNumber");
        vendorId = Objects.requireNonNull(vendorId, "vendorId must not be null");
        vendorName = requireText(vendorName, "vendorName");
        vendorEmail = normalizeOptional(vendorEmail);
        vendorTaxCode = normalizeOptional(vendorTaxCode);
        awardedQuoteId = Objects.requireNonNull(awardedQuoteId, "awardedQuoteId must not be null");
        totalAmount = Objects.requireNonNull(totalAmount, "totalAmount must not be null");
        if (!totalAmount.isPositive()) {
            throw new IllegalArgumentException("totalAmount must be positive");
        }
        paymentTerms = normalizeOptional(paymentTerms);
        awardedBy = Objects.requireNonNull(awardedBy, "awardedBy must not be null");
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        lineItems = List.copyOf(lineItems == null ? List.of() : lineItems);
        if (lineItems.isEmpty()) {
            throw new IllegalArgumentException("lineItems must not be empty");
        }
    }

    public record LineItem(
            UUID rfqLineItemId,
            UUID prLineItemId,
            String itemName,
            String categoryCode,
            BigDecimal quantity,
            String unit,
            Money unitPrice,
            Money totalPrice,
            Integer deliveryDays,
            String warranty) {

        public LineItem {
            rfqLineItemId = Objects.requireNonNull(rfqLineItemId, "rfqLineItemId must not be null");
            prLineItemId = Objects.requireNonNull(prLineItemId, "prLineItemId must not be null");
            itemName = requireText(itemName, "itemName");
            categoryCode = requireText(categoryCode, "categoryCode");
            quantity = Objects.requireNonNull(quantity, "quantity must not be null");
            unit = requireText(unit, "unit");
            unitPrice = Objects.requireNonNull(unitPrice, "unitPrice must not be null");
            totalPrice = Objects.requireNonNull(totalPrice, "totalPrice must not be null");
            if (!unitPrice.currency().equals(totalPrice.currency())) {
                throw new IllegalArgumentException("line item currency mismatch");
            }
            warranty = normalizeOptional(warranty);
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
