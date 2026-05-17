package com.eprocure.iam.application.service;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record SessionData(String tokenHash, UUID userId, Instant expiresAt, Set<String> permissions) {
    public boolean isActive(Instant now) {
        return expiresAt.isAfter(now);
    }
}
