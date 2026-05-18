package com.eprocure.iam.application.service;

import java.time.Instant;
import java.util.Set;

public record CreatedSession(String rawToken, String tokenHash, Instant expiresAt, Set<String> roles) {
    public CreatedSession {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }
}
