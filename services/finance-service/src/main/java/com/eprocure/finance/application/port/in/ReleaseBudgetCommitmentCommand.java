package com.eprocure.finance.application.port.in;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ReleaseBudgetCommitmentCommand(
        String eventId,
        String topic,
        Integer partitionId,
        Long offsetValue,
        UUID purchaseRequestId,
        String prNumber,
        Instant occurredAt) {

    public ReleaseBudgetCommitmentCommand {
        eventId = requireText(eventId, "eventId");
        topic = requireText(topic, "topic");
        purchaseRequestId = Objects.requireNonNull(purchaseRequestId, "purchaseRequestId must not be null");
        prNumber = requireText(prNumber, "prNumber");
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
