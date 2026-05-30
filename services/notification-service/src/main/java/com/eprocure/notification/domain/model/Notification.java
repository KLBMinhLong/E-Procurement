package com.eprocure.notification.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Notification(
        UUID id,
        UUID recipientId,
        String eventType,
        NotificationChannel channel,
        String subject,
        String body,
        String referenceType,
        UUID referenceId,
        String referenceNumber,
        String actionUrl,
        NotificationStatus status,
        boolean read,
        Instant readAt,
        Instant sentAt,
        short retryCount,
        String lastError,
        Instant createdAt,
        Instant updatedAt,
        UUID createdBy,
        boolean deleted,
        Instant deletedAt,
        UUID deletedBy) {

    public Notification {
        id = Objects.requireNonNull(id, "id must not be null");
        recipientId = Objects.requireNonNull(recipientId, "recipientId must not be null");
        eventType = requireText(eventType, "eventType").toUpperCase();
        channel = Objects.requireNonNull(channel, "channel must not be null");
        body = requireText(body, "body");
        status = Objects.requireNonNull(status, "status must not be null");
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        subject = normalizeNullable(subject);
        referenceType = normalizeNullable(referenceType);
        referenceNumber = normalizeNullable(referenceNumber);
        actionUrl = normalizeActionUrl(actionUrl);
        lastError = normalizeNullable(lastError);
    }

    public static Notification createInApp(
            UUID recipientId,
            String eventType,
            String subject,
            String body,
            String referenceType,
            UUID referenceId,
            String referenceNumber,
            String actionUrl,
            Instant now) {
        return new Notification(
                UUID.randomUUID(),
                recipientId,
                eventType,
                NotificationChannel.IN_APP,
                subject,
                body,
                referenceType,
                referenceId,
                referenceNumber,
                actionUrl,
                NotificationStatus.SENT,
                false,
                null,
                now,
                (short) 0,
                null,
                now,
                now,
                recipientId,
                false,
                null,
                null);
    }

    public Notification markRead(Instant readAt) {
        Objects.requireNonNull(readAt, "readAt must not be null");
        if (read) {
            return this;
        }
        return new Notification(
                id,
                recipientId,
                eventType,
                channel,
                subject,
                body,
                referenceType,
                referenceId,
                referenceNumber,
                actionUrl,
                status,
                true,
                readAt,
                sentAt,
                retryCount,
                lastError,
                createdAt,
                readAt,
                createdBy,
                deleted,
                deletedAt,
                deletedBy);
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

    private static String normalizeActionUrl(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            return null;
        }
        if (!normalized.startsWith("/") || normalized.startsWith("//")) {
            throw new IllegalArgumentException("actionUrl must be a relative route");
        }
        return normalized;
    }
}
