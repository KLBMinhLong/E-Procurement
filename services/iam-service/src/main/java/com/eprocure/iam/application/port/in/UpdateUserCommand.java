package com.eprocure.iam.application.port.in;

import java.util.UUID;

public record UpdateUserCommand(
        UUID actorId,
        UUID userId,
        String fullName,
        String phone,
        UUID departmentId,
        UUID orgNodeId) {
}
