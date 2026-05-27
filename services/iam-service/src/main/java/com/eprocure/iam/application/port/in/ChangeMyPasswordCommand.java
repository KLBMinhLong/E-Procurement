package com.eprocure.iam.application.port.in;

import java.util.UUID;

public record ChangeMyPasswordCommand(
        UUID userId,
        String oldPassword,
        String newPassword,
        String confirmPassword) {
}
