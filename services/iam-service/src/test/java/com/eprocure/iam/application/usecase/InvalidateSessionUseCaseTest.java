package com.eprocure.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eprocure.iam.application.port.in.InvalidateSessionCommand;
import com.eprocure.iam.application.port.out.SessionCachePort;
import com.eprocure.iam.application.service.IdempotencyGuard;
import com.eprocure.iam.application.service.SessionData;
import com.eprocure.iam.application.service.SessionService;
import com.eprocure.iam.common.exception.BusinessException;
import com.eprocure.iam.common.exception.ErrorCode;
import com.eprocure.iam.domain.model.ActiveSession;
import com.eprocure.iam.domain.model.SessionRecord;
import com.eprocure.iam.domain.repository.Page;
import com.eprocure.iam.domain.repository.SessionRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InvalidateSessionUseCaseTest {
    private static final UUID ACTOR_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID SESSION_ID = UUID.fromString("20000000-0000-4000-8000-000000000001");
    private static final UUID USER_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final String IDEMPOTENCY_KEY = "11111111-1111-4111-8111-111111111111";
    private static final String TOKEN_HASH = "a".repeat(64);
    private static final Instant NOW = Instant.parse("2026-06-15T00:00:00Z");

    private FakeSessionRepository sessionRepository;
    private FakeSessionCachePort sessionCachePort;
    private InvalidateSessionUseCase useCase;

    @BeforeEach
    void setUp() {
        sessionRepository = new FakeSessionRepository();
        sessionCachePort = new FakeSessionCachePort();
        SessionService sessionService = new SessionService(
                sessionRepository,
                null,
                sessionCachePort,
                null,
                null,
                8);
        useCase = new InvalidateSessionUseCase(
                sessionService,
                new IdempotencyGuard(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Invalidate active session khi command hợp lệ")
    void should_invalidate_session_when_command_is_valid() {
        // When
        useCase.execute(new InvalidateSessionCommand(SESSION_ID, ACTOR_ID, "Suspicious activity"), IDEMPOTENCY_KEY);

        // Then
        assertThat(sessionRepository.revokedSessionId).isEqualTo(SESSION_ID);
        assertThat(sessionRepository.revokedBy).isEqualTo(ACTOR_ID);
        assertThat(sessionRepository.revokedAt).isEqualTo(NOW);
        assertThat(sessionCachePort.evictedTokenHash).isEqualTo(TOKEN_HASH);
    }

    @Test
    @DisplayName("Ném IAM_005 khi reason trống")
    void should_throw_when_reason_is_blank() {
        assertThatThrownBy(() -> useCase.execute(new InvalidateSessionCommand(SESSION_ID, ACTOR_ID, " "), IDEMPOTENCY_KEY))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getErrorCode())
                        .isEqualTo(ErrorCode.IAM_005));
        assertThat(sessionRepository.revokedSessionId).isNull();
        assertThat(sessionCachePort.evictedTokenHash).isNull();
    }

    @Test
    @DisplayName("Ném SYS_005 khi Idempotency-Key không hợp lệ")
    void should_throw_when_idempotency_key_is_invalid() {
        assertThatThrownBy(() -> useCase.execute(new InvalidateSessionCommand(SESSION_ID, ACTOR_ID, "Reason"), "invalid"))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getErrorCode())
                        .isEqualTo(ErrorCode.SYS_005));
        assertThat(sessionRepository.revokedSessionId).isNull();
        assertThat(sessionCachePort.evictedTokenHash).isNull();
    }

    private static final class FakeSessionRepository implements SessionRepository {
        private UUID revokedSessionId;
        private UUID revokedBy;
        private Instant revokedAt;

        @Override
        public void save(SessionRecord sessionRecord) {
        }

        @Override
        public void revokeActiveByUserId(UUID userId, UUID revokedBy, Instant revokedAt) {
        }

        @Override
        public void revokeByTokenHash(String tokenHash, UUID revokedBy, Instant revokedAt) {
        }

        @Override
        public void revokeById(UUID sessionId, UUID revokedBy, Instant revokedAt) {
            this.revokedSessionId = sessionId;
            this.revokedBy = revokedBy;
            this.revokedAt = revokedAt;
        }

        @Override
        public Optional<SessionRecord> findActiveByTokenHash(String tokenHash, Instant now) {
            return Optional.empty();
        }

        @Override
        public Optional<SessionRecord> findActiveById(UUID sessionId, Instant now) {
            if (!SESSION_ID.equals(sessionId)) {
                return Optional.empty();
            }
            return Optional.of(SessionRecord.issue(
                    SESSION_ID,
                    USER_ID,
                    TOKEN_HASH,
                    "10.0.0.15",
                    "Mozilla/5.0",
                    NOW.minusSeconds(60),
                    NOW.plusSeconds(3600)));
        }

        @Override
        public List<String> findActiveTokenHashesByUserId(UUID userId, Instant now) {
            return List.of();
        }

        @Override
        public Page<ActiveSession> findActivePage(UUID userId, int offset, int limit, Instant now) {
            return new Page<>(List.of(), 0);
        }
    }

    private static final class FakeSessionCachePort implements SessionCachePort {
        private String evictedTokenHash;

        @Override
        public void store(SessionData sessionData, Duration ttl) {
        }

        @Override
        public Optional<SessionData> findByTokenHash(String tokenHash) {
            return Optional.empty();
        }

        @Override
        public void evict(String tokenHash) {
            this.evictedTokenHash = tokenHash;
        }
    }
}
