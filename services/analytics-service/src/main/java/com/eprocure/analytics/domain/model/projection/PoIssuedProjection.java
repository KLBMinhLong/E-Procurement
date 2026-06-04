package com.eprocure.analytics.domain.model.projection;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record PoIssuedProjection(
        AnalyticsEventMetadata eventMetadata,
        UUID poId,
        String poNumber,
        UUID prId,
        String prNumber,
        UUID vendorId,
        String vendorName,
        BigDecimal totalAmount,
        String currency,
        Instant issuedAt,
        List<PoIssuedLineProjection> lineItems) {

    public PoIssuedProjection {
        eventMetadata = Objects.requireNonNull(eventMetadata, "eventMetadata must not be null");
        poId = Objects.requireNonNull(poId, "poId must not be null");
        poNumber = requireText(poNumber, "poNumber");
        vendorName = defaultText(vendorName, "UNKNOWN_VENDOR");
        totalAmount = totalAmount == null ? BigDecimal.ZERO : totalAmount;
        currency = normalizeCurrency(currency);
        issuedAt = issuedAt == null ? eventMetadata.eventTimestamp() : issuedAt;
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
