package com.eprocure.iam.presentation.auth;

import java.util.UUID;

public record LoginResponse(UUID userId, String fullName, String avatarUrl, boolean requiresTwoFactor) {
}
