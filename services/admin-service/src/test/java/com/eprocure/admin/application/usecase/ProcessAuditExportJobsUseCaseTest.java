package com.eprocure.admin.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.eprocure.admin.application.port.out.AuditExportFileRenderer;
import com.eprocure.admin.application.port.out.RenderedAuditExport;
import com.eprocure.admin.domain.model.AuditActor;
import com.eprocure.admin.domain.model.AuditExportJob;
import com.eprocure.admin.domain.model.AuditLogEntry;
import com.eprocure.admin.domain.model.AuditLogFilter;
import com.eprocure.admin.domain.repository.AuditExportJobRepository;
import com.eprocure.admin.domain.repository.AuditLogRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessAuditExportJobsUseCaseTest {
    private static final UUID ACTOR_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID FILTER_ACTOR_ID = UUID.fromString("20000000-0000-4000-8000-000000000001");
    private static final UUID JOB_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final UUID IDEMPOTENCY_KEY = UUID.fromString("40000000-0000-4000-8000-000000000001");
    private static final Instant NOW = Instant.parse("2026-06-15T00:00:00Z");
    private static final Instant FROM_TIME = Instant.parse("2026-06-01T00:00:00Z");
    private static final Instant TO_TIME = Instant.parse("2026-06-15T00:00:00Z");
    private static final int MAX_ROWS = 1000;

    @Mock
    private AuditExportJobRepository exportJobRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AuditExportFileRenderer renderer;

    private ProcessAuditExportJobsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ProcessAuditExportJobsUseCase(
                exportJobRepository,
                auditLogRepository,
                renderer,
                Clock.fixed(NOW, ZoneOffset.UTC),
                MAX_ROWS);
    }

    @Test
    @DisplayName("Worker render file và đánh dấu completed khi job queued hợp lệ")
    void should_mark_completed_when_queued_job_is_rendered() {
        // Given
        AuditExportJob job = queuedJob();
        List<AuditLogEntry> entries = List.of(entry());
        given(exportJobRepository.claimQueuedForProcessing(5, NOW)).willReturn(List.of(job));
        given(auditLogRepository.findForExport(any(AuditLogFilter.class), eq(MAX_ROWS))).willReturn(entries);
        given(renderer.render(job, entries)).willReturn(new RenderedAuditExport("audit.xlsx", "D:/tmp/audit.xlsx"));

        // When
        int processed = useCase.execute(5);

        // Then
        assertThat(processed).isEqualTo(1);
        ArgumentCaptor<AuditLogFilter> filterCaptor = ArgumentCaptor.forClass(AuditLogFilter.class);
        verify(auditLogRepository).findForExport(filterCaptor.capture(), eq(MAX_ROWS));
        assertThat(filterCaptor.getValue().actorId()).isEqualTo(FILTER_ACTOR_ID);
        assertThat(filterCaptor.getValue().entityType()).isEqualTo("SERVICE_CONFIG");
        assertThat(filterCaptor.getValue().action()).isEqualTo("CONFIG.UPDATED");
        verify(exportJobRepository).markCompleted(
                JOB_ID,
                "audit.xlsx",
                "D:/tmp/audit.xlsx",
                NOW,
                NOW.plusSeconds(7 * 24 * 60 * 60));
        verify(exportJobRepository, never()).markFailed(eq(JOB_ID), any(), any());
    }

    @Test
    @DisplayName("Worker đánh dấu failed khi renderer lỗi")
    void should_mark_failed_when_renderer_fails() {
        // Given
        AuditExportJob job = queuedJob();
        List<AuditLogEntry> entries = List.of(entry());
        given(exportJobRepository.claimQueuedForProcessing(1, NOW)).willReturn(List.of(job));
        given(auditLogRepository.findForExport(any(AuditLogFilter.class), eq(MAX_ROWS))).willReturn(entries);
        given(renderer.render(job, entries)).willThrow(new IllegalStateException("Cannot render audit export file"));

        // When
        int processed = useCase.execute(1);

        // Then
        assertThat(processed).isEqualTo(1);
        ArgumentCaptor<String> reasonCaptor = ArgumentCaptor.forClass(String.class);
        verify(exportJobRepository).markFailed(eq(JOB_ID), reasonCaptor.capture(), eq(NOW));
        assertThat(reasonCaptor.getValue()).isEqualTo("Cannot render audit export file");
        verify(exportJobRepository, never()).markCompleted(eq(JOB_ID), any(), any(), any(), any());
    }

    private AuditExportJob queuedJob() {
        return AuditExportJob.queued(
                JOB_ID,
                FROM_TIME,
                TO_TIME,
                Optional.of(FILTER_ACTOR_ID),
                Optional.of("SERVICE_CONFIG"),
                Optional.of("CONFIG.UPDATED"),
                IDEMPOTENCY_KEY,
                NOW,
                NOW.plusSeconds(3600),
                ACTOR_ID);
    }

    private AuditLogEntry entry() {
        return new AuditLogEntry(
                1L,
                new AuditActor(FILTER_ACTOR_ID, "Super Admin", List.of("SUPER_ADMIN"), "127.0.0.1"),
                "CONFIG.UPDATED",
                "SERVICE_CONFIG",
                Optional.empty(),
                Optional.of("admin-service"),
                FROM_TIME,
                Optional.of("PUT"),
                Optional.of("/api/v1/admin/config/services/admin-service"),
                Optional.of("request-1"),
                true,
                Optional.empty(),
                Optional.of("{\"enabled\":false}"),
                Optional.of("{\"enabled\":true}"),
                Optional.of("Enabled admin config"),
                "admin-service");
    }
}
