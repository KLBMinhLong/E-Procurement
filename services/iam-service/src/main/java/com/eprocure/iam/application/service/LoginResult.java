package com.eprocure.iam.application.service;

import java.util.UUID;

public record LoginResult(UUID userId, String fullName, String avatarUrl, boolean requiresTwoFactor, String rawToken) {
}
