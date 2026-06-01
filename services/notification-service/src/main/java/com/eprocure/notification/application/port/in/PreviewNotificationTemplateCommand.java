package com.eprocure.notification.application.port.in;

import java.util.Map;
import java.util.UUID;

public record PreviewNotificationTemplateCommand(
        String code,
        Map<String, String> variables,
        UUID actorId) {
}
