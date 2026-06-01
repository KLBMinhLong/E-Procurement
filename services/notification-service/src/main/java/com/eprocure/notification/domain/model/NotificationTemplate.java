package com.eprocure.notification.domain.model;

import java.time.Instant;
import java.util.Objects;

public record NotificationTemplate(
        String code,
        String eventType,
        NotificationChannel channel,
        String language,
        String subjectTemplate,
        String bodyTemplate,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    public NotificationTemplate {
        code = requireText(code, "code").toUpperCase();
        eventType = requireText(eventType, "eventType").toUpperCase();
        channel = Objects.requireNonNull(channel, "channel must not be null");
        language = requireText(language, "language").toLowerCase();
        bodyTemplate = requireText(bodyTemplate, "bodyTemplate");
        subjectTemplate = subjectTemplate == null || subjectTemplate.isBlank() ? null : subjectTemplate.trim();
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
