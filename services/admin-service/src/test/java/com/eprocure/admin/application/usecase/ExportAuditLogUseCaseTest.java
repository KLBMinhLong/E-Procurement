package com.eprocure.admin.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.eprocure.admin.application.port.in.ExportAuditLogCommand;
import com.eprocure.admin.application.service.IdempotencyGuard;
import com.eprocure.admin.common.exception.BusinessException;
import com.eprocure.admin.common.exception.ErrorCode;
import com.eprocure.admin.domain.model.AuditExportJob;
import com.eprocure.admin.domain.model.AuditExportJobStatus;
import com.eprocure.admin.domain.repository.AuditExportJobRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
class ExportAuditLogUseCaseTest {
    private static final UUID ACTOR_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID FILTER_ACTOR_ID = UUID.fromString("20000000-0000-4000-8000-000000000001");
    private static final String IDEMPOTENCY_KEY = "11111111-1111-4111-8111-111111111111";
    private static final UUID IDEMPOTENCY_UUID = UUID.fromString(IDEMPOTENCY_KEY);
    private static final Instant NOW = Instant.parse("2026-06-15T00:00:00Z");
    private static final Instant FROM_TIME = Instant.parse("2026-06-01T00:00:00Z");
    private static final Instant TO_TIME = Instant.parse("2026-06-15T00:00:00Z");

    @Mock
    private AuditExportJobRepository repository;

    private ExportAuditLogUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ExportAuditLogUseCase(
                repository,
                new IdempotencyGuard(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Tạo audit export job queued khi idempotency key mới")
    void should_create_queued_export_job_when_idempotency_key_is_new() {
        // Given
        ExportAuditLogCommand command = command(FROM_TIME, TO_TIME);
        given(repository.findByIdempotencyKey(ACTOR_ID, IDEMPOTENCY_UUID)).willReturn(Optional.empty());
        given(repository.saveQueued(org.mockito.ArgumentMatchers.any(AuditExportJob.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // When
        var result = useCase.execute(command, IDEMPOTENCY_KEY);

        // Then
        assertThat(result.replayed()).isFalse();
        assertThat(result.job().status()).isEqualTo(AuditExportJobStatus.QUEUED);
        assertThat(result.job().requestedAt()).isEqualTo(NOW);
        assertThat(result.job().expiresAt()).contains(NOW.plusSeconds(7 * 24 * 60 * 60));

        ArgumentCaptor<AuditExportJob> captor = ArgumentCaptor.forClass(AuditExportJob.class);
        verify(repository).saveQueued(captor.capture());
        assertThat(captor.getValue().filterActorId()).contains(FILTER_ACTOR_ID);
        assertThat(captor.getValue().entityType()).contains("SERVICE_CONFIG");
        assertThat(captor.getValue().action()).contains("CONFIG.UPDATED");
    }

    @Test
    @DisplayName("Replay audit export job khi Idempotency-Key đã tồn tại")
    void should_replay_existing_export_job_when_idempotency_key_exists() {
        // Given
        AuditExportJob existing = job();
        given(repository.findByIdempotencyKey(ACTOR_ID, IDEMPOTENCY_UUID)).willReturn(Optional.of(existing));

        // When
        var result = useCase.execute(command(FROM_TIME, TO_TIME), IDEMPOTENCY_KEY);

        // Then
        assertThat(result.replayed()).isTrue();
        assertThat(result.job()).isEqualTo(existing);
        verify(repository).findByIdempotencyKey(ACTOR_ID, IDEMPOTENCY_UUID);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("Ném SYS_005 khi Idempotency-Key không hợp lệ")
    void should_throw_when_idempotency_key_is_invalid() {
        assertThatThrownBy(() -> useCase.execute(command(FROM_TIME, TO_TIME), "not-a-uuid"))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getErrorCode())
                        .isEqualTo(ErrorCode.SYS_005.code()));
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("Ném VAL_001 khi khoảng thời gian export không hợp lệ")
    void should_throw_when_export_time_range_is_invalid() {
        // Given
        given(repository.findByIdempotencyKey(ACTOR_ID, IDEMPOTENCY_UUID)).willReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> useCase.execute(command(TO_TIME, FROM_TIME), IDEMPOTENCY_KEY))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getErrorCode())
                        .isEqualTo(ErrorCode.VAL_001.code()));
        verify(repository).findByIdempotencyKey(ACTOR_ID, IDEMPOTENCY_UUID);
        verifyNoMoreInteractions(repository);
    }

    private ExportAuditLogCommand command(Instant fromTime, Instant toTime) {
        return new ExportAuditLogCommand(
                ACTOR_ID,
                fromTime,
                toTime,
                Optional.of(FILTER_ACTOR_ID),
                Optional.of(" SERVICE_CONFIG "),
                Optional.of(" CONFIG.UPDATED "));
    }

    private AuditExportJob job() {
        return AuditExportJob.queued(
                UUID.fromString("30000000-0000-4000-8000-000000000001"),
                FROM_TIME,
                TO_TIME,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                IDEMPOTENCY_UUID,
                NOW,
                NOW.plusSeconds(7 * 24 * 60 * 60),
                ACTOR_ID);
    }
}
