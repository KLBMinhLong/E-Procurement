package com.eprocure.admin.application.port.in;

import com.eprocure.admin.application.service.AdminAuditContext;
import java.util.Objects;
import java.util.UUID;

public record ManageDepartmentCommand(
        UUID actorId,
        UUID departmentId,
        String code,
        String name,
        UUID parentId,
        UUID headUserId,
        String glAccountPrefix,
        AdminAuditContext auditContext) {

    public ManageDepartmentCommand {
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        code = code == null ? "" : code.trim();
        name = name == null ? "" : name.trim();
        glAccountPrefix = normalizeNullable(glAccountPrefix);
        auditContext = auditContext == null ? AdminAuditContext.system(actorId) : auditContext;
    }

    private static String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
