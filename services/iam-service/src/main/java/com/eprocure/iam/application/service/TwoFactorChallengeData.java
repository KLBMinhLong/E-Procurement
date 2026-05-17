package com.eprocure.iam.application.service;

import java.time.Instant;
import java.util.UUID;

public record TwoFactorChallengeData(
        String tokenHash,
        UUID userId,
        String ipAddress,
        String userAgent,
        Instant expiresAt) {

    public boolean isActive(Instant now) {
        return expiresAt.isAfter(now);
    }
}
