package com.eprocure.analytics.presentation.response;

import com.eprocure.analytics.domain.model.report.ReportFormat;
import com.eprocure.analytics.domain.model.report.ReportJobStatus;
import com.eprocure.analytics.domain.model.report.ReportType;
import java.time.Instant;
import java.util.UUID;

public record ReportJobResponse(
        UUID jobId,
        ReportType reportType,
        ReportJobStatus status,
        ReportFormat format,
        String downloadUrl,
        Instant createdAt,
        Instant completedAt,
        Instant expiresAt) {
}
