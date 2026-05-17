package com.eprocure.iam.application.service;

import java.time.Instant;
import java.util.Set;

public record CreatedSession(String rawToken, String tokenHash, Instant expiresAt, Set<String> permissions) {
}
