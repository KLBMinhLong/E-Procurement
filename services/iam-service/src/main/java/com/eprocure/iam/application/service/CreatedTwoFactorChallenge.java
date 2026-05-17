package com.eprocure.iam.application.service;

import java.time.Instant;

public record CreatedTwoFactorChallenge(String rawToken, String tokenHash, Instant expiresAt) {
}
