package com.eprocure.analytics.infrastructure.report;

import com.eprocure.analytics.application.port.out.ReportDataset;
import com.eprocure.analytics.application.port.out.ReportDatasetProvider;
import com.eprocure.analytics.application.port.out.ReportDatasetRow;
import com.eprocure.analytics.domain.model.report.ReportFilterCriteria;
import com.eprocure.analytics.domain.model.report.ReportJob;
import com.eprocure.analytics.domain.model.report.ReportType;
import com.eprocure.analytics.infrastructure.persistence.entity.ReportDatasetRowDbEntity;
import com.eprocure.analytics.infrastructure.persistence.mapper.ReportDatasetMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
        ReportFilterCriteria filters = job.filterCriteria();
        Instant fromInclusive = fromInclusive(filters);
        Instant toExclusive = toExclusive(filters);
        List<ReportDatasetRow> rows = switch (job.reportType()) {
            case PO_SUMMARY -> toRows(mapper.findPoSummaryRows(
                    fromInclusive,
                    toExclusive,
                    filters.vendorId(),
                    filters.categoryCode()));
            case PR_SUMMARY -> toRows(mapper.findPrSummaryRows(
                    fromInclusive,
                    toExclusive,
                    filters.vendorId(),
                    filters.categoryCode()));
            case SLA_COMPLIANCE -> toRows(mapper.findSlaComplianceRows(fromInclusive, toExclusive));
            case THREE_WAY_MATCH -> toRows(mapper.findThreeWayMatchRows(
                    fromInclusive,
                    toExclusive,
                    filters.vendorId()));
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

    private Instant fromInclusive(ReportFilterCriteria filters) {
        if (filters.fromDate() != null) {
            return atStartOfDay(filters.fromDate());
        }
        if (filters.fiscalYear() == null) {
            return null;
        }
        int month = filters.quarter() == null ? 1 : ((filters.quarter() - 1) * 3) + 1;
        return atStartOfDay(LocalDate.of(filters.fiscalYear(), month, 1));
    }

    private Instant toExclusive(ReportFilterCriteria filters) {
        if (filters.toDate() != null) {
            return atStartOfDay(filters.toDate().plusDays(1));
        }
        if (filters.fiscalYear() == null) {
            return null;
        }
        if (filters.quarter() == null) {
            return atStartOfDay(LocalDate.of(filters.fiscalYear() + 1, 1, 1));
        }
        int month = ((filters.quarter() - 1) * 3) + 1;
        return atStartOfDay(LocalDate.of(filters.fiscalYear(), month, 1).plusMonths(3));
    }

    private Instant atStartOfDay(LocalDate date) {
        return date.atStartOfDay().toInstant(ZoneOffset.UTC);
    }
}
