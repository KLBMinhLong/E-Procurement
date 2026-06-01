package com.eprocure.notification.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateNotificationTemplateRequest(
        String subjectTemplate,
        @NotBlank String bodyTemplate,
        @NotNull Boolean isActive) {
}
