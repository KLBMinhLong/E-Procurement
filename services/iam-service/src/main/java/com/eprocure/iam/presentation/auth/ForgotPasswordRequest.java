package com.eprocure.iam.presentation.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForgotPasswordRequest(@NotBlank @Email(message = "Invalid email format") @Size(max = 255) String email) {
}
