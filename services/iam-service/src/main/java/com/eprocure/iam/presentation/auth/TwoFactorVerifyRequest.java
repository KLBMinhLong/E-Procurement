package com.eprocure.iam.presentation.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TwoFactorVerifyRequest(
        @NotBlank
        @Pattern(regexp = "^\\d{6}$")
        String code) {
}
