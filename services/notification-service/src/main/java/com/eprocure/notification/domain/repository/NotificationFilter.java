package com.eprocure.notification.domain.repository;

import java.util.UUID;

public record NotificationFilter(
        UUID recipientId,
        Boolean read,
        String eventType,
        int page,
        int size,
        int offset) {
    public NotificationFilter {
        if (recipientId == null) {
            throw new IllegalArgumentException("recipientId must not be null");
        }
        eventType = eventType == null || eventType.isBlank() ? null : eventType.trim().toUpperCase();
        page = Math.max(page, 1);
        size = Math.min(Math.max(size, 1), 100);
        offset = Math.max(offset, 0);
    }
}
