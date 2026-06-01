package com.eprocure.notification.application.port.in;

import java.util.UUID;

public record UpdateNotificationTemplateCommand(
        String code,
        String subjectTemplate,
        String bodyTemplate,
        boolean active,
        UUID actorId) {
}
