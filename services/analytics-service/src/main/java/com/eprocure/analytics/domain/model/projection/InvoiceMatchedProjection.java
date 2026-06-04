package com.eprocure.analytics.domain.model.projection;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record InvoiceMatchedProjection(
        AnalyticsEventMetadata eventMetadata,
        UUID invoiceId,
        String invoiceNumber,
        UUID poId,
        String poNumber,
        UUID vendorId,
        String vendorName,
        BigDecimal totalAmount,
        String currency,
        LocalDate dueDate,
        Instant matchedAt) {

    public InvoiceMatchedProjection {
        eventMetadata = Objects.requireNonNull(eventMetadata, "eventMetadata must not be null");
        invoiceId = Objects.requireNonNull(invoiceId, "invoiceId must not be null");
        invoiceNumber = requireText(invoiceNumber, "invoiceNumber");
        poId = Objects.requireNonNull(poId, "poId must not be null");
        poNumber = requireText(poNumber, "poNumber");
        vendorName = defaultText(vendorName, "UNKNOWN_VENDOR");
        totalAmount = totalAmount == null ? BigDecimal.ZERO : totalAmount;
        currency = normalizeCurrency(currency);
        matchedAt = matchedAt == null ? eventMetadata.eventTimestamp() : matchedAt;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String normalizeCurrency(String value) {
        String normalized = defaultText(value, "VND").toUpperCase();
        if (normalized.length() != 3) {
            throw new IllegalArgumentException("currency must be a 3-letter code");
        }
        return normalized;
    }
}
