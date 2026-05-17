package com.eprocure.iam.presentation.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TwoFactorConfirmRequest(
        @NotBlank
        @Pattern(regexp = "^\\d{6}$")
        String code) {
}
