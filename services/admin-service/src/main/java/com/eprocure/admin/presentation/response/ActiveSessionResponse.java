package com.eprocure.admin.presentation.response;

import java.time.Instant;
import java.util.UUID;

public record ActiveSessionResponse(
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
