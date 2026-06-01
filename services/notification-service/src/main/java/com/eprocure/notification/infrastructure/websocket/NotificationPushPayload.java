package com.eprocure.notification.infrastructure.websocket;

import com.eprocure.notification.application.service.NotificationView;
import java.time.Instant;
import java.util.UUID;

public record NotificationPushPayload(
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

    public static NotificationPushPayload from(NotificationView view) {
        return new NotificationPushPayload(
                view.id(),
                view.eventType(),
                view.channel(),
                view.subject(),
                view.body(),
                view.read(),
                view.readAt(),
                view.referenceType(),
                view.referenceId(),
                view.referenceNumber(),
                view.actionUrl(),
                view.createdAt());
    }
}
