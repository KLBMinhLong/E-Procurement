package com.eprocure.admin.application.port.in;

import com.eprocure.admin.application.service.AdminAuditContext;
import java.util.Objects;
import java.util.UUID;

public record DeactivateDepartmentCommand(UUID actorId, UUID departmentId, AdminAuditContext auditContext) {

    public DeactivateDepartmentCommand {
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        departmentId = Objects.requireNonNull(departmentId, "departmentId must not be null");
        auditContext = auditContext == null ? AdminAuditContext.system(actorId) : auditContext;
    }
}
