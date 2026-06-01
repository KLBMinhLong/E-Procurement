package com.eprocure.notification.presentation.response;

import com.eprocure.notification.domain.model.NotificationChannel;
import java.time.Instant;

public record NotificationTemplateResponse(
        String code,
        String eventType,
        NotificationChannel channel,
        String language,
        String subjectTemplate,
        String bodyTemplate,
        boolean isActive,
        Instant updatedAt) {
}
