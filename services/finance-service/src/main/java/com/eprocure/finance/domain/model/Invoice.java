package com.eprocure.finance.domain.model;

import com.eprocure.finance.domain.model.vo.Money;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record Invoice(
        UUID id,
        String invoiceNumber,
        UUID vendorId,
        String vendorName,
        UUID poId,
        String poNumber,
        List<InvoiceLineItem> lineItems,
        Money subtotal,
        Money taxAmount,
        Money totalAmount,
        LocalDate invoiceDate,
        LocalDate dueDate,
        InvoiceStatus status,
        MatchStatus poMatchStatus,
        MatchStatus grMatchStatus,
        java.math.BigDecimal qtyVariance,
        Money priceVariance,
        Instant matchedAt,
        UUID matchedBy,
        UUID approvedBy,
        Instant approvedAt,
        Instant createdAt,
        UUID createdBy,
        UUID idempotencyKey) {

    public Invoice {
        id = Objects.requireNonNull(id, "id must not be null");
        invoiceNumber = requireText(invoiceNumber, "invoiceNumber");
        vendorId = Objects.requireNonNull(vendorId, "vendorId must not be null");
        vendorName = requireText(vendorName, "vendorName");
        poId = Objects.requireNonNull(poId, "poId must not be null");
        poNumber = requireText(poNumber, "poNumber");
        lineItems = List.copyOf(lineItems == null ? List.of() : lineItems);
        if (lineItems.isEmpty()) {
            throw new IllegalArgumentException("lineItems must not be empty");
        }
        subtotal = Objects.requireNonNull(subtotal, "subtotal must not be null");
        taxAmount = Objects.requireNonNull(taxAmount, "taxAmount must not be null");
        totalAmount = Objects.requireNonNull(totalAmount, "totalAmount must not be null");
        verifyTotals(lineItems, subtotal, taxAmount, totalAmount);
        invoiceDate = Objects.requireNonNull(invoiceDate, "invoiceDate must not be null");
        dueDate = Objects.requireNonNull(dueDate, "dueDate must not be null");
        if (dueDate.isBefore(invoiceDate)) {
            throw new IllegalArgumentException("dueDate must not be before invoiceDate");
        }
        status = Objects.requireNonNull(status, "status must not be null");
        if (priceVariance != null && !priceVariance.currency().equals(totalAmount.currency())) {
            throw new IllegalArgumentException("priceVariance currency mismatch");
        }
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        createdBy = Objects.requireNonNull(createdBy, "createdBy must not be null");
        idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
    }

    public static Invoice create(
            String invoiceNumber,
            UUID vendorId,
            String vendorName,
            UUID poId,
            String poNumber,
            List<InvoiceLineItem> lineItems,
            LocalDate invoiceDate,
            LocalDate dueDate,
            Instant createdAt,
            UUID createdBy,
            UUID idempotencyKey,
            String currency) {
        Money subtotal = lineItems.stream()
                .map(InvoiceLineItem::totalPrice)
                .reduce(Money.zero(currency), Money::add);
        Money taxAmount = lineItems.stream()
                .map(InvoiceLineItem::taxAmount)
                .reduce(Money.zero(currency), Money::add);
        return new Invoice(
                UUID.randomUUID(),
                invoiceNumber,
                vendorId,
                vendorName,
                poId,
                poNumber,
                lineItems,
                subtotal,
                taxAmount,
                subtotal.add(taxAmount),
                invoiceDate,
                dueDate,
                InvoiceStatus.PENDING_MATCH,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                createdAt,
                createdBy,
                idempotencyKey);
    }

    private static void verifyTotals(
            List<InvoiceLineItem> lineItems,
            Money subtotal,
            Money taxAmount,
            Money totalAmount) {
        Money lineSubtotal = lineItems.stream()
                .map(InvoiceLineItem::totalPrice)
                .reduce(Money.zero(totalAmount.currency()), Money::add);
        Money lineTax = lineItems.stream()
                .map(InvoiceLineItem::taxAmount)
                .reduce(Money.zero(totalAmount.currency()), Money::add);
        if (lineSubtotal.compareTo(subtotal) != 0 || lineTax.compareTo(taxAmount) != 0) {
            throw new IllegalArgumentException("invoice totals must equal line totals");
        }
        if (subtotal.add(taxAmount).compareTo(totalAmount) != 0) {
            throw new IllegalArgumentException("totalAmount must equal subtotal plus taxAmount");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
