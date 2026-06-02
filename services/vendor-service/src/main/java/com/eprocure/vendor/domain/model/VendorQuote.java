package com.eprocure.vendor.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record VendorQuote(
        UUID id,
        UUID rfqId,
        UUID vendorId,
        String vendorName,
        List<VendorQuoteLineItem> lineItems,
        BigDecimal totalAmount,
        String currency,
        LocalDate validUntil,
        String paymentTerms,
        String notes,
        Instant submittedAt,
        BigDecimal evaluationScore,
        String evaluationNote,
        UUID evaluatedBy,
        Instant evaluatedAt,
        UUID idempotencyKey,
        Instant createdAt,
        UUID createdBy,
        UUID updatedBy) {

    public VendorQuote {
        id = Objects.requireNonNull(id, "id must not be null");
        rfqId = Objects.requireNonNull(rfqId, "rfqId must not be null");
        vendorId = Objects.requireNonNull(vendorId, "vendorId must not be null");
        vendorName = requireText(vendorName, "vendorName");
        lineItems = List.copyOf(lineItems == null ? List.of() : lineItems);
        if (lineItems.isEmpty()) {
            throw new IllegalArgumentException("lineItems must not be empty");
        }
        currency = requireCurrency(currency);
        totalAmount = totalAmount == null ? calculateTotal(lineItems) : normalizeAmount(totalAmount, "totalAmount");
        validUntil = Objects.requireNonNull(validUntil, "validUntil must not be null");
        paymentTerms = normalizeNullable(paymentTerms);
        notes = normalizeNullable(notes);
        submittedAt = Objects.requireNonNull(submittedAt, "submittedAt must not be null");
        evaluationScore = evaluationScore == null ? null : normalizeScore(evaluationScore);
        evaluationNote = normalizeNullable(evaluationNote);
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        createdBy = Objects.requireNonNull(createdBy, "createdBy must not be null");
    }

    public static VendorQuote create(
            UUID id,
            UUID rfqId,
            UUID vendorId,
            String vendorName,
            List<VendorQuoteLineItem> lineItems,
            String currency,
            LocalDate validUntil,
            String paymentTerms,
            String notes,
            UUID actorId,
            UUID idempotencyKey,
            Instant submittedAt) {
        return new VendorQuote(
                id,
                rfqId,
                vendorId,
                vendorName,
                lineItems,
                null,
                currency,
                validUntil,
                paymentTerms,
                notes,
                submittedAt,
                null,
                null,
                null,
                null,
                Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null"),
                submittedAt,
                actorId,
                null);
    }

    public VendorQuote evaluate(BigDecimal score, String note, UUID evaluatorId, Instant evaluatedAt) {
        Objects.requireNonNull(evaluatorId, "evaluatorId must not be null");
        Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
        return new VendorQuote(
                id,
                rfqId,
                vendorId,
                vendorName,
                lineItems,
                totalAmount,
                currency,
                validUntil,
                paymentTerms,
                notes,
                submittedAt,
                normalizeScore(score),
                note,
                evaluatorId,
                evaluatedAt,
                idempotencyKey,
                createdAt,
                createdBy,
                evaluatorId);
    }

    private static BigDecimal calculateTotal(List<VendorQuoteLineItem> items) {
        return items.stream()
                .map(VendorQuoteLineItem::totalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);
    }

    private static BigDecimal normalizeAmount(BigDecimal value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.signum() <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private static BigDecimal normalizeScore(BigDecimal value) {
        Objects.requireNonNull(value, "evaluationScore must not be null");
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("evaluationScore must be between 0 and 100");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static String requireCurrency(String value) {
        String currency = requireText(value, "currency").toUpperCase(Locale.ROOT);
        if (currency.length() != 3) {
            throw new IllegalArgumentException("currency must be ISO 4217 code");
        }
        return currency;
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
