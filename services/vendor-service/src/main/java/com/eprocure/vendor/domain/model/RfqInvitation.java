package com.eprocure.vendor.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record RfqInvitation(
        UUID id,
        UUID vendorId,
        String vendorName,
        Instant invitedAt,
        boolean hasSubmitted,
        Instant submittedAt) {

    public RfqInvitation {
        id = Objects.requireNonNull(id, "id must not be null");
        vendorId = Objects.requireNonNull(vendorId, "vendorId must not be null");
        vendorName = requireText(vendorName, "vendorName");
        invitedAt = Objects.requireNonNull(invitedAt, "invitedAt must not be null");
        if (!hasSubmitted && submittedAt != null) {
            throw new IllegalArgumentException("submittedAt requires hasSubmitted=true");
        }
    }

    public static RfqInvitation create(UUID vendorId, String vendorName, Instant invitedAt) {
        return new RfqInvitation(UUID.randomUUID(), vendorId, vendorName, invitedAt, false, null);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
