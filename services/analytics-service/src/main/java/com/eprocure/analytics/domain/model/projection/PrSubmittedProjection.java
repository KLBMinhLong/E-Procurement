package com.eprocure.analytics.domain.model.projection;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PrSubmittedProjection(
        AnalyticsEventMetadata eventMetadata,
        UUID purchaseRequestId,
        String prNumber,
        UUID requesterId,
        UUID departmentId,
        String priority,
        int fiscalYear,
        BigDecimal totalAmount,
        String currency,
        Instant submittedAt) {

    public PrSubmittedProjection {
        eventMetadata = Objects.requireNonNull(eventMetadata, "eventMetadata must not be null");
        purchaseRequestId = Objects.requireNonNull(purchaseRequestId, "purchaseRequestId must not be null");
        prNumber = requireText(prNumber, "prNumber");
        requesterId = Objects.requireNonNull(requesterId, "requesterId must not be null");
        departmentId = Objects.requireNonNull(departmentId, "departmentId must not be null");
        priority = requireText(priority, "priority").toUpperCase();
        if (fiscalYear < 2000 || fiscalYear > 2100) {
            throw new IllegalArgumentException("fiscalYear is out of range");
        }
        totalAmount = totalAmount == null ? BigDecimal.ZERO : totalAmount;
        currency = normalizeCurrency(currency);
        submittedAt = submittedAt == null ? eventMetadata.eventTimestamp() : submittedAt;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeCurrency(String value) {
        String normalized = value == null || value.isBlank() ? "VND" : value.trim().toUpperCase();
        if (normalized.length() != 3) {
            throw new IllegalArgumentException("currency must be a 3-letter code");
        }
        return normalized;
    }
}
