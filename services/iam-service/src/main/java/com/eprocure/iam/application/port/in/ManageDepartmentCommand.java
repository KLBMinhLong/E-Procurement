package com.eprocure.iam.application.port.in;

import java.util.UUID;

public record ManageDepartmentCommand(
        UUID actorId,
        UUID departmentId,
        String code,
        String name,
        UUID parentId,
        UUID headUserId) {
}
