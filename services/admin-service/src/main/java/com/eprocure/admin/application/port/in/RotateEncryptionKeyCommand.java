package com.eprocure.admin.application.port.in;

import com.eprocure.admin.application.service.AdminAuditContext;
import java.util.Objects;
import java.util.UUID;

public record RotateEncryptionKeyCommand(
        UUID actorId,
        String confirmationCode,
        int keySize,
        AdminAuditContext auditContext) {

    public RotateEncryptionKeyCommand {
        Objects.requireNonNull(actorId, "actorId must not be null");
        confirmationCode = confirmationCode == null ? "" : confirmationCode.trim();
        auditContext = auditContext == null ? AdminAuditContext.system(actorId) : auditContext;
    }
}
