package com.eprocure.admin.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.eprocure.admin.application.port.in.GetAuditExportJobQuery;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetAuditExportDownloadUseCaseTest {
    private static final UUID ACTOR_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID JOB_ID = UUID.fromString("20000000-0000-4000-8000-000000000001");
    private static final UUID IDEMPOTENCY_KEY = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final Instant NOW = Instant.parse("2026-06-15T00:00:00Z");
    private static final Instant FROM_TIME = Instant.parse("2026-06-01T00:00:00Z");
    private static final Instant TO_TIME = Instant.parse("2026-06-15T00:00:00Z");

    @Mock
    private AuditExportJobRepository repository;

    private GetAuditExportDownloadUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetAuditExportDownloadUseCase(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Trả về job download khi export đã hoàn tất và chưa hết hạn")
    void should_return_job_when_export_file_is_ready() {
        // Given
        AuditExportJob completed = job(AuditExportJobStatus.COMPLETED, Optional.of("audit.xlsx"), Optional.of("D:/tmp/audit.xlsx"), Optional.empty(), NOW.plusSeconds(3600));
        given(repository.findByIdAndActorId(JOB_ID, ACTOR_ID)).willReturn(Optional.of(completed));

        // When
        AuditExportJob result = useCase.execute(query());

        // Then
        assertThat(result).isEqualTo(completed);
    }

    @Test
    @DisplayName("Ném SYS_012 khi export chưa hoàn tất")
    void should_throw_when_export_file_is_not_ready() {
        // Given
        AuditExportJob queued = job(AuditExportJobStatus.QUEUED, Optional.empty(), Optional.empty(), Optional.empty(), NOW.plusSeconds(3600));
        given(repository.findByIdAndActorId(JOB_ID, ACTOR_ID)).willReturn(Optional.of(queued));

        // When / Then
        assertThatThrownBy(() -> useCase.execute(query()))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getErrorCode())
                        .isEqualTo(ErrorCode.AUDIT_EXPORT_FILE_NOT_READY.code()));
    }

    @Test
    @DisplayName("Ném SYS_011 khi không tìm thấy job thuộc user")
    void should_throw_when_export_job_is_not_found() {
        // Given
        given(repository.findByIdAndActorId(JOB_ID, ACTOR_ID)).willReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> useCase.execute(query()))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getErrorCode())
                        .isEqualTo(ErrorCode.AUDIT_EXPORT_JOB_NOT_FOUND.code()));
    }

    private GetAuditExportJobQuery query() {
        return new GetAuditExportJobQuery(ACTOR_ID, JOB_ID);
    }

    private AuditExportJob job(
            AuditExportJobStatus status,
            Optional<String> fileName,
            Optional<String> storagePath,
            Optional<String> failureReason,
            Instant expiresAt) {
        return new AuditExportJob(
                JOB_ID,
                status,
                FROM_TIME,
                TO_TIME,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                fileName,
                storagePath,
                failureReason,
                IDEMPOTENCY_KEY,
                NOW,
                status == AuditExportJobStatus.COMPLETED ? Optional.of(NOW) : Optional.empty(),
                Optional.of(expiresAt),
                ACTOR_ID);
    }
}
