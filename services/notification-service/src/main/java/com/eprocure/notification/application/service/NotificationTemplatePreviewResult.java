package com.eprocure.notification.application.service;

public record NotificationTemplatePreviewResult(
        String subject,
        String htmlBody,
        boolean replayed) {
}
