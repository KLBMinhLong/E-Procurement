package com.eprocure.iam.application.port.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AdminResetPasswordCommand(
        @NotNull(message = "Actor ID is required")
        UUID actorId,
        @NotNull(message = "Target user ID is required")
        UUID targetUserId,
        @NotBlank(message = "New password cannot be blank")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        String newPassword
) {
}
