package com.eprocure.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.eprocure.iam.application.port.in.ListActiveSessionsQuery;
import com.eprocure.iam.domain.model.ActiveSession;
import com.eprocure.iam.domain.repository.Page;
import com.eprocure.iam.domain.repository.SessionRepository;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListActiveSessionsUseCaseTest {
    private static final UUID USER_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID SESSION_ID = UUID.fromString("20000000-0000-4000-8000-000000000001");
    private static final Instant NOW = Instant.parse("2026-06-15T00:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-06-15T08:00:00Z");

    @Mock
    private SessionRepository sessionRepository;

    private ListActiveSessionsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListActiveSessionsUseCase(sessionRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Trả về active sessions với pagination khi query hợp lệ")
    void should_return_active_sessions_when_query_is_valid() {
        // Given
        var query = new ListActiveSessionsQuery(1, 20, USER_ID);
        given(sessionRepository.findActivePage(USER_ID, 0, 20, NOW))
                .willReturn(new Page<>(List.of(session()), 1));

        // When
        var result = useCase.execute(query);

        // Then
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).sessionId()).isEqualTo(SESSION_ID);
        assertThat(result.meta().totalElements()).isEqualTo(1);
        verify(sessionRepository).findActivePage(USER_ID, 0, 20, NOW);
    }

    private ActiveSession session() {
        return new ActiveSession(
                SESSION_ID,
                USER_ID,
                "superadmin",
                "Super Admin",
                Optional.of("127.0.0.1"),
                Optional.of("Mozilla"),
                NOW,
                NOW,
                EXPIRES_AT);
    }
}
