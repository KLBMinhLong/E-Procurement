package com.eprocure.analytics.presentation.request;

import com.eprocure.analytics.domain.model.report.ReportFormat;
import com.eprocure.analytics.domain.model.report.ReportType;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

public record ExportReportRequest(
        @NotNull ReportType reportType,
        @NotNull ReportFormat format,
        JsonNode filters) {
}
