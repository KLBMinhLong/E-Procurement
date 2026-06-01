package com.eprocure.iam.infrastructure.notification;

import com.eprocure.iam.domain.model.User;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record PasswordResetEmailEvent(
        String eventId,
        String eventType,
        String source,
        Instant timestamp,
        Payload payload) {
    private static final String EMAIL_SEND_EVENT_TYPE = "notification.email.send";
    private static final String PASSWORD_RESET_TEMPLATE_EVENT_TYPE = "PASSWORD_RESET";
    private static final String SOURCE = "iam-service";

    public PasswordResetEmailEvent {
        eventId = requireText(eventId, "eventId");
        eventType = requireText(eventType, "eventType");
        source = requireText(source, "source");
        timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
        payload = Objects.requireNonNull(payload, "payload must not be null");
    }

    public static PasswordResetEmailEvent create(
            User user,
            String resetUrl,
            Instant expiresAt,
            Instant occurredAt,
            String language) {
        Objects.requireNonNull(user, "user must not be null");
        return new PasswordResetEmailEvent(
                UUID.randomUUID().toString(),
                EMAIL_SEND_EVENT_TYPE,
                SOURCE,
                Objects.requireNonNull(occurredAt, "occurredAt must not be null"),
                new Payload(
                        PASSWORD_RESET_TEMPLATE_EVENT_TYPE,
                        user.getId().toString(),
                        user.getEmail(),
                        requireText(resetUrl, "resetUrl"),
                        Objects.requireNonNull(expiresAt, "expiresAt must not be null").toString(),
                        normalizeLanguage(language),
                        "USER",
                        user.getId().toString()));
    }

    private static String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return "vi";
        }
        return language.trim().toLowerCase(Locale.ROOT);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    public record Payload(
            String templateEventType,
            String recipientId,
            String recipientEmail,
            String resetUrl,
            String expiresAt,
            String language,
            String referenceType,
            String referenceId) {
        public Payload {
            templateEventType = requireText(templateEventType, "templateEventType");
            recipientId = requireText(recipientId, "recipientId");
            recipientEmail = requireText(recipientEmail, "recipientEmail");
            resetUrl = requireText(resetUrl, "resetUrl");
            expiresAt = requireText(expiresAt, "expiresAt");
            language = normalizeLanguage(language);
            referenceType = requireText(referenceType, "referenceType");
            referenceId = requireText(referenceId, "referenceId");
        }
    }
}
