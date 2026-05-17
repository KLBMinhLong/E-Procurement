package com.eprocure.iam.application.port.in;

import java.util.List;
import java.util.UUID;

public record CreateRoleCommand(
        UUID actorId,
        String code,
        String name,
        String description,
        List<String> permissions) {
    public CreateRoleCommand {
        permissions = permissions == null ? List.of() : List.copyOf(permissions);
    }
}
