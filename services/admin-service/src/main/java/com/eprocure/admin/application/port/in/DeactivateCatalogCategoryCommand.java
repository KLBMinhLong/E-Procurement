package com.eprocure.admin.application.port.in;

import com.eprocure.admin.application.service.AdminAuditContext;
import java.util.Objects;
import java.util.UUID;

public record DeactivateCatalogCategoryCommand(UUID actorId, String code, AdminAuditContext auditContext) {
    public DeactivateCatalogCategoryCommand {
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        code = code == null ? "" : code.trim();
        auditContext = auditContext == null ? AdminAuditContext.system(actorId) : auditContext;
    }
}
