package com.eprocure.analytics.infrastructure.report;

import com.eprocure.analytics.application.port.out.ReportDataset;
import com.eprocure.analytics.application.port.out.ReportDatasetProvider;
import com.eprocure.analytics.application.port.out.ReportDatasetRow;
import com.eprocure.analytics.domain.model.report.ReportJob;
import com.eprocure.analytics.domain.model.report.ReportType;
import com.eprocure.analytics.infrastructure.persistence.entity.ReportDatasetRowDbEntity;
import com.eprocure.analytics.infrastructure.persistence.mapper.ReportDatasetMapper;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProjectionReportDatasetProvider implements ReportDatasetProvider {
    private final ReportDatasetMapper mapper;

    public ProjectionReportDatasetProvider(ReportDatasetMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ReportDataset load(ReportJob job) {
        List<ReportDatasetRow> rows = switch (job.reportType()) {
            case PO_SUMMARY -> toRows(mapper.findPoSummaryRows());
            case PR_SUMMARY -> toRows(mapper.findPrSummaryRows());
            case SLA_COMPLIANCE -> toRows(mapper.findSlaComplianceRows());
            case THREE_WAY_MATCH -> toRows(mapper.findThreeWayMatchRows());
            case CYCLE_TIME_ANALYSIS -> List.of(
                    new ReportDatasetRow("Cycle time source", "Pending PR lifecycle projection"),
                    new ReportDatasetRow("Current status", "Foundation only"));
            default -> List.of(
                    new ReportDatasetRow("Dataset source", "Pending projection contract"),
                    new ReportDatasetRow("Current status", "Foundation only"));
        };
        return new ReportDataset(rows);
    }

    private List<ReportDatasetRow> toRows(List<ReportDatasetRowDbEntity> rows) {
        return rows.stream()
                .map(row -> row.toDomain())
                .toList();
    }
}
