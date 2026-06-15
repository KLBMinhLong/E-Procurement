package com.eprocure.admin.application.service;

import java.time.Instant;
import java.util.UUID;

public record ActiveSessionView(
        UUID sessionId,
        UUID userId,
        String userName,
        String fullName,
        String ipAddress,
        String userAgent,
        Instant createdAt,
        Instant lastActivity,
        Instant expiresAt) {
}
