package com.eprocure.notification.application.service;

import com.eprocure.notification.domain.model.NotificationChannel;
import com.eprocure.notification.domain.model.NotificationTemplate;
import java.time.Instant;

public record NotificationTemplateView(
        String code,
        String eventType,
        NotificationChannel channel,
        String language,
        String subjectTemplate,
        String bodyTemplate,
        boolean active,
        Instant updatedAt) {

    public static NotificationTemplateView from(NotificationTemplate template) {
        return new NotificationTemplateView(
                template.code(),
                template.eventType(),
                template.channel(),
                template.language(),
                template.subjectTemplate(),
                template.bodyTemplate(),
                template.active(),
                template.updatedAt());
    }
}
