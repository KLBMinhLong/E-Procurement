package com.eprocure.iam.application.port.in;

import java.util.List;
import java.util.UUID;

public record CreateUserCommand(
        UUID actorId,
        String employeeCode,
        String username,
        String email,
        String fullName,
        String phone,
        UUID departmentId,
        UUID orgNodeId,
        List<String> roles) {
    public CreateUserCommand {
        roles = roles == null || roles.isEmpty() ? List.of("REQUESTER") : List.copyOf(roles);
    }
}
