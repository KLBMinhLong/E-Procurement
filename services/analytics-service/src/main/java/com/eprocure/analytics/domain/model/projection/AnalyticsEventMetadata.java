package com.eprocure.analytics.domain.model.projection;

import java.time.Instant;
import java.util.Objects;

public record AnalyticsEventMetadata(
        String eventId,
        String topic,
        int partitionId,
        long offsetValue,
        Instant eventTimestamp) {

    public AnalyticsEventMetadata {
        eventId = requireText(eventId, "eventId");
        topic = requireText(topic, "topic");
        if (partitionId < 0) {
            throw new IllegalArgumentException("partitionId must not be negative");
        }
        if (offsetValue < 0) {
            throw new IllegalArgumentException("offsetValue must not be negative");
        }
        eventTimestamp = Objects.requireNonNull(eventTimestamp, "eventTimestamp must not be null");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
