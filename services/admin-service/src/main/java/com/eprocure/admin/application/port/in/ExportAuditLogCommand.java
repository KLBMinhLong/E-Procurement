package com.eprocure.admin.application.port.in;

import com.eprocure.admin.application.service.AdminAuditContext;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record ExportAuditLogCommand(
        UUID requestedBy,
        Instant fromTime,
        Instant toTime,
        Optional<UUID> filterActorId,
        Optional<String> entityType,
        Optional<String> action,
        AdminAuditContext auditContext) {

    public ExportAuditLogCommand {
        requestedBy = Objects.requireNonNull(requestedBy, "requestedBy must not be null");
        filterActorId = filterActorId == null ? Optional.empty() : filterActorId;
        entityType = normalize(entityType);
        action = normalize(action);
        auditContext = auditContext == null ? AdminAuditContext.system(requestedBy) : auditContext;
    }

    private static Optional<String> normalize(Optional<String> value) {
        if (value == null || value.isEmpty() || value.get().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value.get().trim());
    }
}
