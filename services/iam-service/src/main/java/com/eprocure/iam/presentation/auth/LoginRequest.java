package com.eprocure.iam.presentation.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password,
        String encryptedPayload,
        String encryptedAesKey,
        String iv,
        String keyVersion) {
}
