package com.eprocure.analytics.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.eprocure.analytics.application.port.out.RenderedReport;
import com.eprocure.analytics.application.port.out.ReportDataset;
import com.eprocure.analytics.application.port.out.ReportDatasetProvider;
import com.eprocure.analytics.application.port.out.ReportDatasetRow;
import com.eprocure.analytics.application.port.out.ReportFileRenderer;
import com.eprocure.analytics.domain.model.report.ReportFormat;
import com.eprocure.analytics.domain.model.report.ReportJob;
import com.eprocure.analytics.domain.model.report.ReportJobStatus;
import com.eprocure.analytics.domain.model.report.ReportType;
import com.eprocure.analytics.domain.repository.ReportJobRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProcessReportExportJobsUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-06-05T04:00:00Z");
    private static final UUID JOB_ID = UUID.fromString("70000000-0000-4000-8000-000000000001");
    private static final UUID ACTOR_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final UUID IDEMPOTENCY_KEY = UUID.fromString("10000000-0000-4000-8000-000000000001");

    @Test
    void should_complete_claimed_report_job_when_renderer_succeeds() {
        FakeReportJobRepository repository = new FakeReportJobRepository();
        repository.claimedJobs = List.of(queuedJob());
        FakeDatasetProvider datasetProvider = new FakeDatasetProvider();
        ProcessReportExportJobsUseCase useCase = new ProcessReportExportJobsUseCase(
                repository,
                datasetProvider,
                (job, dataset) -> new RenderedReport("D:/tmp/report.pdf", "/api/v1/reports/jobs/" + job.id() + "/download"),
                Clock.fixed(NOW, ZoneOffset.UTC));

        int processed = useCase.execute(5);

        assertThat(processed).isEqualTo(1);
        assertThat(repository.claimLimit).isEqualTo(5);
        assertThat(repository.completedJobId).isEqualTo(JOB_ID);
        assertThat(repository.completedDownloadUrl).isEqualTo("/api/v1/reports/jobs/" + JOB_ID + "/download");
        assertThat(repository.completedStoragePath).isEqualTo("D:/tmp/report.pdf");
        assertThat(repository.completedAt).isEqualTo(NOW);
        assertThat(repository.expiresAt).isEqualTo(NOW.plusSeconds(604800));
        assertThat(datasetProvider.loadedJobId).isEqualTo(JOB_ID);
    }

    @Test
    void should_mark_job_failed_when_renderer_fails() {
        FakeReportJobRepository repository = new FakeReportJobRepository();
        repository.claimedJobs = List.of(queuedJob());
        ReportFileRenderer failingRenderer = (job, dataset) -> {
            throw new IllegalStateException("render failed");
        };
        ProcessReportExportJobsUseCase useCase = new ProcessReportExportJobsUseCase(
                repository,
                job -> new ReportDataset(List.of(new ReportDatasetRow("Metric", "Value"))),
                failingRenderer,
                Clock.fixed(NOW, ZoneOffset.UTC));

        int processed = useCase.execute(5);

        assertThat(processed).isEqualTo(1);
        assertThat(repository.failedJobId).isEqualTo(JOB_ID);
        assertThat(repository.failureReason).isEqualTo("render failed");
        assertThat(repository.failedAt).isEqualTo(NOW);
    }

    private ReportJob queuedJob() {
        return ReportJob.queued(
                JOB_ID,
                ReportType.PO_SUMMARY,
                ReportFormat.PDF,
                NOW.minusSeconds(60),
                NOW.plusSeconds(3600),
                ACTOR_ID,
                IDEMPOTENCY_KEY);
    }

    private static final class FakeReportJobRepository implements ReportJobRepository {
        private List<ReportJob> claimedJobs = List.of();
        private int claimLimit;
        private UUID completedJobId;
        private String completedDownloadUrl;
        private String completedStoragePath;
        private Instant completedAt;
        private Instant expiresAt;
        private UUID failedJobId;
        private String failureReason;
        private Instant failedAt;

        @Override
        public Optional<ReportJob> findByIdempotencyKey(UUID actorId, UUID idempotencyKey) {
            return Optional.empty();
        }

        @Override
        public Optional<ReportJob> findByIdAndActorId(UUID jobId, UUID actorId) {
            return Optional.empty();
        }

        @Override
        public void saveQueued(ReportJob job, JsonNode filters) {
        }

        @Override
        public List<ReportJob> claimQueuedForProcessing(int limit, Instant claimedAt) {
            this.claimLimit = limit;
            return claimedJobs;
        }

        @Override
        public void markCompleted(UUID jobId, String downloadUrl, String storagePath, Instant completedAt, Instant expiresAt) {
            this.completedJobId = jobId;
            this.completedDownloadUrl = downloadUrl;
            this.completedStoragePath = storagePath;
            this.completedAt = completedAt;
            this.expiresAt = expiresAt;
        }

        @Override
        public void markFailed(UUID jobId, String failureReason, Instant failedAt) {
            this.failedJobId = jobId;
            this.failureReason = failureReason;
            this.failedAt = failedAt;
        }
    }

    private static final class FakeDatasetProvider implements ReportDatasetProvider {
        private UUID loadedJobId;

        @Override
        public ReportDataset load(ReportJob job) {
            loadedJobId = job.id();
            return new ReportDataset(List.of(new ReportDatasetRow("Metric", "Value")));
        }
    }
}
