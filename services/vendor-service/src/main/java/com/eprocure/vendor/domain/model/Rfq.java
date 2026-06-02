package com.eprocure.vendor.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record Rfq(
        UUID id,
        String rfqNumber,
        UUID prId,
        String prNumber,
        String title,
        RfqStatus status,
        Instant submissionDeadline,
        List<RfqLineItem> lineItems,
        List<RfqInvitation> invitations,
        UUID awardedVendorId,
        UUID awardedQuoteId,
        String awardReason,
        String requirements,
        Instant closedAt,
        UUID idempotencyKey,
        Instant createdAt,
        UUID createdBy,
        UUID updatedBy) {

    public Rfq {
        id = Objects.requireNonNull(id, "id must not be null");
        rfqNumber = requireText(rfqNumber, "rfqNumber");
        prId = Objects.requireNonNull(prId, "prId must not be null");
        prNumber = requireText(prNumber, "prNumber");
        title = requireText(title, "title");
        status = Objects.requireNonNull(status, "status must not be null");
        submissionDeadline = Objects.requireNonNull(submissionDeadline, "submissionDeadline must not be null");
        lineItems = List.copyOf(lineItems == null ? List.of() : lineItems);
        invitations = List.copyOf(invitations == null ? List.of() : invitations);
        if (lineItems.isEmpty()) {
            throw new IllegalArgumentException("lineItems must not be empty");
        }
        if (invitations.size() < 2) {
            throw new IllegalArgumentException("at least 2 vendor invitations are required");
        }
        awardReason = normalizeNullable(awardReason);
        requirements = normalizeNullable(requirements);
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        createdBy = Objects.requireNonNull(createdBy, "createdBy must not be null");
    }

    public static Rfq create(
            UUID id,
            String rfqNumber,
            UUID prId,
            String prNumber,
            String title,
            Instant submissionDeadline,
            List<RfqLineItem> lineItems,
            List<RfqInvitation> invitations,
            String requirements,
            UUID actorId,
            UUID idempotencyKey,
            Instant createdAt) {
        if (!submissionDeadline.isAfter(createdAt)) {
            throw new IllegalArgumentException("submissionDeadline must be in the future");
        }
        return new Rfq(
                id,
                rfqNumber,
                prId,
                prNumber,
                title,
                RfqStatus.PUBLISHED,
                submissionDeadline,
                lineItems,
                invitations,
                null,
                null,
                null,
                requirements,
                null,
                Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null"),
                createdAt,
                actorId,
                null);
    }

    public Rfq close(UUID actorId, Instant closedAt) {
        Objects.requireNonNull(actorId, "actorId must not be null");
        Objects.requireNonNull(closedAt, "closedAt must not be null");
        if (status == RfqStatus.CLOSED) {
            return this;
        }
        if (status != RfqStatus.PUBLISHED) {
            throw new IllegalStateException("only PUBLISHED RFQ can be closed");
        }
        return new Rfq(
                id,
                rfqNumber,
                prId,
                prNumber,
                title,
                RfqStatus.CLOSED,
                submissionDeadline,
                lineItems,
                invitations,
                awardedVendorId,
                awardedQuoteId,
                awardReason,
                requirements,
                closedAt,
                idempotencyKey,
                createdAt,
                createdBy,
                actorId);
    }

    public Rfq award(UUID quoteId, UUID vendorId, String reason, UUID actorId, Instant awardedAt) {
        Objects.requireNonNull(quoteId, "quoteId must not be null");
        Objects.requireNonNull(vendorId, "vendorId must not be null");
        Objects.requireNonNull(actorId, "actorId must not be null");
        Objects.requireNonNull(awardedAt, "awardedAt must not be null");
        if (status == RfqStatus.AWARDED && quoteId.equals(awardedQuoteId)) {
            return this;
        }
        if (status != RfqStatus.CLOSED && !(status == RfqStatus.PUBLISHED && !submissionDeadline.isAfter(awardedAt))) {
            throw new IllegalStateException("only CLOSED or expired PUBLISHED RFQ can be awarded");
        }
        return new Rfq(
                id,
                rfqNumber,
                prId,
                prNumber,
                title,
                RfqStatus.AWARDED,
                submissionDeadline,
                lineItems,
                invitations,
                vendorId,
                quoteId,
                requireText(reason, "awardReason"),
                requirements,
                closedAt == null ? awardedAt : closedAt,
                idempotencyKey,
                createdAt,
                createdBy,
                actorId);
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
