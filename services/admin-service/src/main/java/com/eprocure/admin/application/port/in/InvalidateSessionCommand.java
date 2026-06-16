package com.eprocure.admin.application.port.in;

import com.eprocure.admin.application.service.AdminAuditContext;
import java.util.Objects;
import java.util.UUID;

public record InvalidateSessionCommand(
        UUID sessionId,
        UUID actorId,
        String reason,
        AdminAuditContext auditContext) {

    public InvalidateSessionCommand {
        sessionId = Objects.requireNonNull(sessionId, "sessionId must not be null");
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        reason = reason == null ? "" : reason.trim();
        auditContext = auditContext == null ? AdminAuditContext.system(actorId) : auditContext;
    }
}
