package com.eprocure.analytics.application.port.in;

import com.eprocure.analytics.domain.model.report.ReportFormat;
import com.eprocure.analytics.domain.model.report.ReportType;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;
import java.util.UUID;

public record ExportReportCommand(
        UUID actorId,
        ReportType reportType,
        ReportFormat format,
        JsonNode filters) {

    public ExportReportCommand {
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        reportType = Objects.requireNonNull(reportType, "reportType must not be null");
        format = Objects.requireNonNull(format, "format must not be null");
    }
}
