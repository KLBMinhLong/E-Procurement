package com.eprocure.admin.application.port.in;

import java.util.UUID;

public record DeactivateDepartmentCommand(UUID actorId, UUID departmentId) {
}
