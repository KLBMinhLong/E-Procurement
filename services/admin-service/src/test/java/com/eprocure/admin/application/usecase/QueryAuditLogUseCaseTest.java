package com.eprocure.admin.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.eprocure.admin.application.service.PageMeta;
import com.eprocure.admin.application.service.PageResult;
import com.eprocure.admin.common.exception.BusinessException;
import com.eprocure.admin.common.exception.ErrorCode;
import com.eprocure.admin.domain.model.AuditActor;
import com.eprocure.admin.domain.model.AuditLogEntry;
import com.eprocure.admin.domain.model.AuditLogFilter;
import com.eprocure.admin.domain.repository.AuditLogRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QueryAuditLogUseCaseTest {
    private static final UUID ACTOR_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final Instant FROM_TIME = Instant.parse("2026-06-15T00:00:00Z");
    private static final Instant TO_TIME = Instant.parse("2026-06-15T23:59:59Z");

    @Mock
    private AuditLogRepository repository;

    @InjectMocks
    private QueryAuditLogUseCase useCase;

    @Test
    @DisplayName("Truy vấn audit log trả về page result khi filter hợp lệ")
    void should_query_audit_log_when_filter_is_valid() {
        // Given
        AuditLogFilter filter = validFilter(FROM_TIME, TO_TIME);
        PageResult<AuditLogEntry> page = new PageResult<>(
                List.of(entry()),
                PageMeta.of(1, 1, 50, "occurredAt,desc"));
        given(repository.findByFilter(filter)).willReturn(page);

        // When
        PageResult<AuditLogEntry> result = useCase.execute(filter, ACTOR_ID);

        // Then
        assertThat(result.items()).hasSize(1);
        assertThat(result.meta().totalElements()).isEqualTo(1);
        verify(repository).findByFilter(filter);
    }

    @Test
    @DisplayName("Ném VAL_001 khi khoảng thời gian audit không hợp lệ")
    void should_throw_when_time_range_is_invalid() {
        // Given
        AuditLogFilter filter = validFilter(TO_TIME, FROM_TIME);

        // When / Then
        assertThatThrownBy(() -> useCase.execute(filter, ACTOR_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getErrorCode())
                        .isEqualTo(ErrorCode.VAL_001.code()));
        verifyNoInteractions(repository);
    }

    private AuditLogFilter validFilter(Instant fromTime, Instant toTime) {
        return new AuditLogFilter(
                null,
                null,
                null,
                null,
                null,
                null,
                fromTime,
                toTime,
                1,
                50,
                0);
    }

    private AuditLogEntry entry() {
        return new AuditLogEntry(
                1L,
                new AuditActor(ACTOR_ID, "Super Admin", List.of("SUPER_ADMIN"), "127.0.0.1"),
                "CONFIG.VIEWED",
                "SYSTEM_CONFIG",
                Optional.empty(),
                Optional.empty(),
                FROM_TIME,
                Optional.of("GET"),
                Optional.of("/api/v1/admin/config/services"),
                Optional.of("request-1"),
                true,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of("Viewed config"),
                "admin-service");
    }
}
