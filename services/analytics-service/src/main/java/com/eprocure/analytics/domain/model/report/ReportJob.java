package com.eprocure.analytics.domain.model.report;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ReportJob(
        UUID id,
        ReportType reportType,
        ReportFormat format,
        ReportJobStatus status,
        String downloadUrl,
        Instant createdAt,
        Instant completedAt,
        Instant expiresAt,
        UUID createdBy,
        UUID idempotencyKey) {

    public ReportJob {
        id = Objects.requireNonNull(id, "id must not be null");
        reportType = Objects.requireNonNull(reportType, "reportType must not be null");
        format = Objects.requireNonNull(format, "format must not be null");
        status = Objects.requireNonNull(status, "status must not be null");
        downloadUrl = downloadUrl == null || downloadUrl.isBlank() ? null : downloadUrl.trim();
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        createdBy = Objects.requireNonNull(createdBy, "createdBy must not be null");
        idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
    }

    public static ReportJob queued(
            UUID id,
            ReportType reportType,
            ReportFormat format,
            Instant createdAt,
            Instant expiresAt,
            UUID createdBy,
            UUID idempotencyKey) {
        return new ReportJob(
                id,
                reportType,
                format,
                ReportJobStatus.QUEUED,
                null,
                createdAt,
                null,
                expiresAt,
                createdBy,
                idempotencyKey);
    }
}
