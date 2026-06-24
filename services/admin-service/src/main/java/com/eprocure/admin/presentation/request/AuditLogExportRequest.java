package com.eprocure.admin.presentation.request;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record AuditLogExportRequest(
        @NotNull Instant fromTime,
        @NotNull Instant toTime,
        UUID actorId,
        String entityType,
        String action) {
}
