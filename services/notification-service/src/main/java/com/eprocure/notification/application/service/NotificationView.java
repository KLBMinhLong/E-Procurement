package com.eprocure.notification.application.service;

import com.eprocure.notification.domain.model.Notification;
import java.time.Instant;
import java.util.UUID;

public record NotificationView(
        UUID id,
        UUID recipientId,
        String eventType,
        String channel,
        String subject,
        String body,
        boolean read,
        Instant readAt,
        String referenceType,
        UUID referenceId,
        String referenceNumber,
        String actionUrl,
        Instant createdAt) {

    public static NotificationView from(Notification notification) {
        return new NotificationView(
                notification.id(),
                notification.recipientId(),
                notification.eventType(),
                notification.channel().name(),
                notification.subject(),
                notification.body(),
                notification.read(),
                notification.readAt(),
                notification.referenceType(),
                notification.referenceId(),
                notification.referenceNumber(),
                notification.actionUrl(),
                notification.createdAt());
    }
}
