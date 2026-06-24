package com.eprocure.analytics.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eprocure.analytics.domain.model.report.ReportFormat;
import com.eprocure.analytics.domain.model.report.ReportJobStatus;
import com.eprocure.analytics.domain.model.report.ReportType;
import com.eprocure.analytics.infrastructure.persistence.entity.ReportJobDbEntity;
import com.eprocure.analytics.infrastructure.persistence.mapper.ReportJobMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReportJobRepositoryImplTest {
    private static final Instant NOW = Instant.parse("2026-06-05T04:00:00Z");
    private static final UUID JOB_ID = UUID.fromString("70000000-0000-4000-8000-000000000001");
    private static final UUID ACTOR_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final UUID IDEMPOTENCY_KEY = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID VENDOR_ID = UUID.fromString("40000000-0000-4000-8000-000000000001");

    @Test
    void should_parse_report_filters_when_claiming_jobs_for_processing() {
        ReportJobMapper mapper = mock(ReportJobMapper.class);
        ReportJobDbEntity row = row();
        row.setFiltersJson("""
                {
                  "from_date": "2026-06-01",
                  "toDate": "2026-06-05",
                  "fiscal_year": 2026,
                  "quarter": 5,
                  "vendor_id": "40000000-0000-4000-8000-000000000001",
                  "category_code": "IT-HARDWARE"
                }
                """);
        when(mapper.selectQueuedForProcessing(1)).thenReturn(List.of(row));
        ReportJobRepositoryImpl repository = new ReportJobRepositoryImpl(mapper, new ObjectMapper());

        var jobs = repository.claimQueuedForProcessing(1, NOW);

        assertThat(jobs).hasSize(1);
        var filters = jobs.get(0).filterCriteria();
        assertThat(filters.fromDate()).isEqualTo(LocalDate.parse("2026-06-01"));
        assertThat(filters.toDate()).isEqualTo(LocalDate.parse("2026-06-05"));
        assertThat(filters.fiscalYear()).isEqualTo(2026);
        assertThat(filters.quarter()).isNull();
        assertThat(filters.vendorId()).isEqualTo(VENDOR_ID);
        assertThat(filters.categoryCode()).isEqualTo("IT-HARDWARE");
        assertThat(jobs.get(0).status()).isEqualTo(ReportJobStatus.PROCESSING);
        verify(mapper).updateStatusToProcessing(List.of(JOB_ID), NOW);
    }

    @Test
    void should_not_update_processing_status_when_no_queued_jobs_exist() {
        ReportJobMapper mapper = mock(ReportJobMapper.class);
        when(mapper.selectQueuedForProcessing(5)).thenReturn(List.of());
        ReportJobRepositoryImpl repository = new ReportJobRepositoryImpl(mapper, new ObjectMapper());

        var jobs = repository.claimQueuedForProcessing(5, NOW);

        assertThat(jobs).isEmpty();
        verify(mapper).selectQueuedForProcessing(5);
        verify(mapper, never()).updateStatusToProcessing(anyList(), any());
    }

    private ReportJobDbEntity row() {
        ReportJobDbEntity row = new ReportJobDbEntity();
        row.setId(JOB_ID);
        row.setReportType(ReportType.PO_SUMMARY.name());
        row.setFormat(ReportFormat.PDF.name());
        row.setStatus(ReportJobStatus.QUEUED.name());
        row.setCreatedAt(NOW.minusSeconds(60));
        row.setExpiresAt(NOW.plusSeconds(3600));
        row.setCreatedBy(ACTOR_ID);
        row.setIdempotencyKey(IDEMPOTENCY_KEY);
        return row;
    }
}
