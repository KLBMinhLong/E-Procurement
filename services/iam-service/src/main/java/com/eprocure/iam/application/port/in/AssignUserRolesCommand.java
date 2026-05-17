package com.eprocure.iam.application.port.in;

import java.util.List;
import java.util.UUID;

public record AssignUserRolesCommand(UUID actorId, UUID userId, List<String> roles) {
    public AssignUserRolesCommand {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }
}
