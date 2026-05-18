package com.eprocure.iam.application.service;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record SessionData(String tokenHash, UUID userId, Instant expiresAt, Set<String> roles) {
    public SessionData {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    public boolean isActive(Instant now) {
        return expiresAt.isAfter(now);
    }
}
