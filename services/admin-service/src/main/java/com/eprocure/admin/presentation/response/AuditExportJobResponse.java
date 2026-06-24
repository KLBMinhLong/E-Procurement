package com.eprocure.admin.presentation.response;

import com.eprocure.admin.domain.model.AuditExportJobStatus;
import java.time.Instant;
import java.util.UUID;

public record AuditExportJobResponse(
        UUID jobId,
        AuditExportJobStatus status,
        Instant fromTime,
        Instant toTime,
        UUID actorId,
        String entityType,
        String action,
        String fileName,
        String downloadUrl,
        String failureReason,
        Instant requestedAt,
        Instant completedAt,
        Instant expiresAt) {
}
