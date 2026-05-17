package com.eprocure.iam.application.port.in;

import java.util.List;
import java.util.UUID;

public record UpdateRolePermissionsCommand(UUID actorId, String roleCode, List<String> permissions) {
    public UpdateRolePermissionsCommand {
        permissions = permissions == null ? List.of() : List.copyOf(permissions);
    }
}
