package com.eprocure.notification.application.port.in;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.Objects;

public record BusinessEventCommand(
        String eventId,
        String eventType,
        String source,
        Instant timestamp,
        String topic,
        Integer partitionId,
        Long offsetValue,
        JsonNode payload) {

    public BusinessEventCommand {
        eventId = requireText(eventId, "eventId");
        eventType = requireText(eventType, "eventType").toUpperCase();
        source = requireText(source, "source");
        timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
        topic = requireText(topic, "topic");
        payload = Objects.requireNonNull(payload, "payload must not be null");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
