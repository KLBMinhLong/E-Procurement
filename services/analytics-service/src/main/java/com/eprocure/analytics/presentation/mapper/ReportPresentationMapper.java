package com.eprocure.analytics.presentation.mapper;

import com.eprocure.analytics.application.port.in.ExportReportCommand;
import com.eprocure.analytics.application.port.in.GetReportJobQuery;
import com.eprocure.analytics.common.security.UserPrincipal;
import com.eprocure.analytics.domain.model.report.ReportJob;
import com.eprocure.analytics.presentation.request.ExportReportRequest;
import com.eprocure.analytics.presentation.response.ReportJobResponse;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ReportPresentationMapper {
    public ExportReportCommand toCommand(UserPrincipal principal, ExportReportRequest request) {
        return new ExportReportCommand(principal.getId(), request.reportType(), request.format(), request.filters());
    }

    public GetReportJobQuery toQuery(UserPrincipal principal, UUID jobId) {
        return new GetReportJobQuery(principal.getId(), jobId);
    }

    public ReportJobResponse toResponse(ReportJob job) {
        return new ReportJobResponse(
                job.id(),
                job.reportType(),
                job.status(),
                job.format(),
                job.downloadUrl(),
                job.createdAt(),
                job.completedAt(),
                job.expiresAt());
    }
}
