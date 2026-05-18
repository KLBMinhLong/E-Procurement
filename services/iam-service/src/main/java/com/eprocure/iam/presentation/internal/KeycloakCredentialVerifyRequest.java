package com.eprocure.iam.presentation.internal;

import jakarta.validation.constraints.NotBlank;

public record KeycloakCredentialVerifyRequest(
        @NotBlank String username,
        @NotBlank String password) {
}
