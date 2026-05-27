package com.eprocure.iam.application.port.in;

import java.util.UUID;

public record UpdateMyProfileCommand(
        UUID userId,
        String fullName,
        String phone,
        String avatarUrl) {
}
