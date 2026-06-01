package com.eprocure.notification.application.service;

public record NotificationTemplateUpdateResult(
        NotificationTemplateView template,
        boolean replayed) {
}
