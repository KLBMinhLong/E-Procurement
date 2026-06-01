package com.eprocure.notification.application.port.in;

import com.eprocure.notification.domain.model.NotificationChannel;

public record ListNotificationTemplatesQuery(
        NotificationChannel channel,
        String eventType,
        String language) {
}
