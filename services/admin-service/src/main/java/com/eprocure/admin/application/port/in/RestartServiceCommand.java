package com.eprocure.admin.application.port.in;

import com.eprocure.admin.application.service.AdminAuditContext;
import java.util.Objects;
import java.util.UUID;

public record RestartServiceCommand(
        UUID actorId,
        String serviceName,
        String confirmationCode,
        String reason,
        AdminAuditContext auditContext) {

    public RestartServiceCommand {
        Objects.requireNonNull(actorId, "actorId must not be null");
        serviceName = serviceName == null ? "" : serviceName.trim();
        confirmationCode = confirmationCode == null ? "" : confirmationCode.trim();
        reason = reason == null ? "" : reason.trim();
        auditContext = auditContext == null ? AdminAuditContext.system(actorId) : auditContext;
    }
}
