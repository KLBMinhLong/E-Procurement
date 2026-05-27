package com.eprocure.iam.application.port.in;

import java.util.UUID;

public record UpdateRoleCommand(
        UUID actorId,
        String currentCode,
        String newCode,
        String name,
        String description) {
}
