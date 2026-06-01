package com.eprocure.notification.presentation.response;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String eventType,
        String channel,
        String subject,
        String body,
        boolean isRead,
        Instant readAt,
        String referenceType,
        UUID referenceId,
        String referenceNumber,
        String actionUrl,
        Instant createdAt) {
}
