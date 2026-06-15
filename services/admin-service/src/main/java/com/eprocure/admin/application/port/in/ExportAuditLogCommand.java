package com.eprocure.admin.application.port.in;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public record ExportAuditLogCommand(
        UUID requestedBy,
        Instant fromTime,
        Instant toTime,
        Optional<UUID> filterActorId,
        Optional<String> entityType,
        Optional<String> action) {

    public ExportAuditLogCommand {
        filterActorId = filterActorId == null ? Optional.empty() : filterActorId;
        entityType = normalize(entityType);
        action = normalize(action);
    }

    private static Optional<String> normalize(Optional<String> value) {
        if (value == null || value.isEmpty() || value.get().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value.get().trim());
    }
}
