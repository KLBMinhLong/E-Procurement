package com.eprocure.iam.presentation.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TwoFactorVerifyRequest(
        @NotBlank
        @Pattern(regexp = "^(?:\\d{6}|[A-Za-z2-9]{4}-[A-Za-z2-9]{4})$")
        String code) {
}

