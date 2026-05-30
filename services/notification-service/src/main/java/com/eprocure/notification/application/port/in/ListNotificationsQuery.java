package com.eprocure.notification.application.port.in;

import java.util.UUID;

public record ListNotificationsQuery(
        UUID recipientId,
        Boolean read,
        String eventType,
        int page,
        int size) {

    public ListNotificationsQuery {
        if (recipientId == null) {
            throw new IllegalArgumentException("recipientId must not be null");
        }
        eventType = eventType == null || eventType.isBlank() ? null : eventType.trim().toUpperCase();
        page = Math.max(page, 1);
        size = Math.min(Math.max(size, 1), 100);
    }
}
