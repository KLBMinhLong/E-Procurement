package com.eprocure.finance.domain.model;

import com.eprocure.finance.domain.model.vo.Money;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record PurchaseOrder(
        UUID id,
        String poNumber,
        UUID prId,
        String prNumber,
        UUID rfqId,
        String rfqNumber,
        UUID awardedQuoteId,
        UUID vendorId,
        String vendorName,
        String vendorEmail,
        String vendorTaxCode,
        UUID purchasingOfficerId,
        String purchasingOfficerFullName,
        PurchaseOrderStatus status,
        List<PurchaseOrderLineItem> lineItems,
        Money totalAmount,
        String deliveryAddress,
        LocalDate deliveryDeadline,
        String paymentTerms,
        Instant issuedAt,
        Instant sentToVendorAt,
        Instant createdAt,
        String sourceEventId) {

    public PurchaseOrder {
        id = Objects.requireNonNull(id, "id must not be null");
        poNumber = requireText(poNumber, "poNumber");
        prId = Objects.requireNonNull(prId, "prId must not be null");
        prNumber = requireText(prNumber, "prNumber");
        vendorId = Objects.requireNonNull(vendorId, "vendorId must not be null");
        vendorName = requireText(vendorName, "vendorName");
        vendorEmail = normalizeOptional(vendorEmail);
        vendorTaxCode = normalizeOptional(vendorTaxCode);
        purchasingOfficerId = Objects.requireNonNull(purchasingOfficerId, "purchasingOfficerId must not be null");
        purchasingOfficerFullName = normalizeOptional(purchasingOfficerFullName);
        status = Objects.requireNonNull(status, "status must not be null");
        lineItems = List.copyOf(lineItems == null ? List.of() : lineItems);
        if (lineItems.isEmpty()) {
            throw new IllegalArgumentException("lineItems must not be empty");
        }
        totalAmount = Objects.requireNonNull(totalAmount, "totalAmount must not be null");
        verifyLineItemTotal(lineItems, totalAmount);
        deliveryAddress = normalizeOptional(deliveryAddress);
        paymentTerms = normalizeOptional(paymentTerms);
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        sourceEventId = normalizeOptional(sourceEventId);
    }

    public boolean isPurchasingOfficer(UUID actorId) {
        return purchasingOfficerId.equals(actorId);
    }

    private static void verifyLineItemTotal(List<PurchaseOrderLineItem> lineItems, Money totalAmount) {
        Money lineTotal = lineItems.stream()
                .map(PurchaseOrderLineItem::totalPrice)
                .reduce(Money.zero(totalAmount.currency()), Money::add);
        if (lineTotal.amount().compareTo(totalAmount.amount()) != 0) {
            throw new IllegalArgumentException("line item total must equal purchase order total");
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
