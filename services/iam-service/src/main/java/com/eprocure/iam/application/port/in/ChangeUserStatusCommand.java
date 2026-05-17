package com.eprocure.iam.application.port.in;

import com.eprocure.iam.domain.model.UserStatus;
import java.util.UUID;

public record ChangeUserStatusCommand(
        UUID actorId,
        UUID userId,
        UserStatus status,
        String reason) {
}
