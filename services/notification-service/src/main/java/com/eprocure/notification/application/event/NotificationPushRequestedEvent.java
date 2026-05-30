package com.eprocure.notification.application.event;

import com.eprocure.notification.application.service.NotificationView;
import java.time.Instant;
import java.util.Objects;

public record NotificationPushRequestedEvent(NotificationView notification, Instant occurredAt) {
    public NotificationPushRequestedEvent {
        notification = Objects.requireNonNull(notification, "notification must not be null");
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
