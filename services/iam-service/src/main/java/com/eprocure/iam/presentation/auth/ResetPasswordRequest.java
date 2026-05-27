package com.eprocure.iam.presentation.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank String resetToken,
        @NotBlank @Size(min = 8, max = 128)
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^])[A-Za-z\\d@$!%*?&#^]{8,128}$", message = "Password must contain at least one uppercase letter, one lowercase letter, one number and one special character")
        String newPassword,
        @NotBlank @Size(min = 8, max = 128) String confirmPassword) {
}
