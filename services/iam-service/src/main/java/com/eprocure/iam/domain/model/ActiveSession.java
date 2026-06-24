package com.eprocure.iam.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record ActiveSession(
        UUID sessionId,
        UUID userId,
        String username,
        String fullName,
        Optional<String> ipAddress,
        Optional<String> userAgent,
        Instant issuedAt,
        Instant lastActivityAt,
        Instant expiresAt) {

    public ActiveSession {
        sessionId = Objects.requireNonNull(sessionId, "sessionId must not be null");
        userId = Objects.requireNonNull(userId, "userId must not be null");
        username = Objects.requireNonNullElse(username, "").trim();
        fullName = Objects.requireNonNullElse(fullName, "").trim();
        ipAddress = normalize(ipAddress);
        userAgent = normalize(userAgent);
        issuedAt = Objects.requireNonNull(issuedAt, "issuedAt must not be null");
        lastActivityAt = lastActivityAt == null ? issuedAt : lastActivityAt;
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    }

    private static Optional<String> normalize(Optional<String> value) {
        if (value == null || value.isEmpty() || value.get().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value.get().trim());
    }
}
