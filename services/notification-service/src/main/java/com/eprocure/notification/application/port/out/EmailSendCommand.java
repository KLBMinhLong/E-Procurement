package com.eprocure.notification.application.port.out;

import java.util.Objects;
import java.util.UUID;

public record EmailSendCommand(
        UUID notificationId,
        String to,
        String subject,
        String body) {

    public EmailSendCommand {
        notificationId = Objects.requireNonNull(notificationId, "notificationId must not be null");
        to = requireText(to, "to").toLowerCase();
        subject = requireText(subject, "subject");
        body = requireText(body, "body");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
