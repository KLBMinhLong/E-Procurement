package com.eprocure.analytics.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eprocure.analytics.application.port.in.ExportReportCommand;
import com.eprocure.analytics.application.port.in.GetReportJobQuery;
import com.eprocure.analytics.common.exception.BusinessException;
import com.eprocure.analytics.common.exception.ErrorCode;
import com.eprocure.analytics.domain.model.report.ReportFormat;
import com.eprocure.analytics.domain.model.report.ReportJob;
import com.eprocure.analytics.domain.model.report.ReportJobStatus;
import com.eprocure.analytics.domain.model.report.ReportType;
import com.eprocure.analytics.domain.repository.ReportJobRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReportJobUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-06-04T04:00:00Z");
    private static final UUID ACTOR_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final UUID JOB_ID = UUID.fromString("70000000-0000-4000-8000-000000000001");
    private static final String IDEMPOTENCY_KEY = "10000000-0000-4000-8000-000000000001";

    @Test
    void should_create_report_job_when_idempotency_key_is_new() {
        FakeReportJobRepository repository = new FakeReportJobRepository();
        ExportReportUseCase useCase = new ExportReportUseCase(repository, Clock.fixed(NOW, ZoneOffset.UTC));

        var result = useCase.execute(command(), IDEMPOTENCY_KEY);

        assertThat(result.replayed()).isFalse();
        assertThat(result.job().status()).isEqualTo(ReportJobStatus.QUEUED);
        assertThat(result.job().createdAt()).isEqualTo(NOW);
        assertThat(result.job().expiresAt()).isEqualTo(NOW.plusSeconds(604800));
        assertThat(repository.savedFilters.path("fiscalYear").asInt()).isEqualTo(2026);
    }

    @Test
    void should_replay_existing_report_job_when_idempotency_key_matches() {
        FakeReportJobRepository repository = new FakeReportJobRepository();
        UUID key = UUID.fromString(IDEMPOTENCY_KEY);
        ReportJob existing = ReportJob.queued(
                JOB_ID,
                ReportType.PO_SUMMARY,
                ReportFormat.PDF,
                NOW.minusSeconds(60),
                NOW.plusSeconds(3600),
                ACTOR_ID,
                key);
        repository.byIdempotencyKey.put(key, existing);
        ExportReportUseCase useCase = new ExportReportUseCase(repository, Clock.fixed(NOW, ZoneOffset.UTC));

        var result = useCase.execute(command(), IDEMPOTENCY_KEY);

        assertThat(result.replayed()).isTrue();
        assertThat(result.job().id()).isEqualTo(JOB_ID);
        assertThat(repository.savedFilters).isNull();
    }

    @Test
    void should_throw_sys_005_when_idempotency_key_is_invalid() {
        FakeReportJobRepository repository = new FakeReportJobRepository();
        ExportReportUseCase useCase = new ExportReportUseCase(repository, Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> useCase.execute(command(), "not-a-uuid"))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.SYS_005);
    }

    @Test
    void should_return_report_job_when_actor_owns_it() {
        FakeReportJobRepository repository = new FakeReportJobRepository();
        UUID key = UUID.fromString(IDEMPOTENCY_KEY);
        ReportJob existing = ReportJob.queued(
                JOB_ID,
                ReportType.PO_SUMMARY,
                ReportFormat.PDF,
                NOW,
                NOW.plusSeconds(3600),
                ACTOR_ID,
                key);
        repository.byJobId.put(JOB_ID, existing);
        GetReportJobUseCase useCase = new GetReportJobUseCase(repository);

        ReportJob result = useCase.execute(new GetReportJobQuery(ACTOR_ID, JOB_ID));

        assertThat(result.id()).isEqualTo(JOB_ID);
    }

    private ExportReportCommand command() {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode filters = objectMapper.createObjectNode().put("fiscalYear", 2026);
        return new ExportReportCommand(ACTOR_ID, ReportType.PO_SUMMARY, ReportFormat.PDF, filters);
    }

    private static final class FakeReportJobRepository implements ReportJobRepository {
        private final Map<UUID, ReportJob> byIdempotencyKey = new HashMap<>();
        private final Map<UUID, ReportJob> byJobId = new HashMap<>();
        private JsonNode savedFilters;

        @Override
        public Optional<ReportJob> findByIdempotencyKey(UUID actorId, UUID idempotencyKey) {
            return Optional.ofNullable(byIdempotencyKey.get(idempotencyKey));
        }

        @Override
        public Optional<ReportJob> findByIdAndActorId(UUID jobId, UUID actorId) {
            return Optional.ofNullable(byJobId.get(jobId))
                    .filter(job -> job.createdBy().equals(actorId));
        }

        @Override
        public void saveQueued(ReportJob job, JsonNode filters) {
            byIdempotencyKey.put(job.idempotencyKey(), job);
            byJobId.put(job.id(), job);
            savedFilters = filters;
        }
    }
}
