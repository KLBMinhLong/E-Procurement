package com.eprocure.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eprocure.iam.application.port.in.ClientContext;
import com.eprocure.iam.application.port.in.LoginCommand;
import com.eprocure.iam.application.port.out.CredentialVerificationPort;
import com.eprocure.iam.application.port.out.SessionCachePort;
import com.eprocure.iam.application.service.LoginResult;
import com.eprocure.iam.application.service.OpaqueTokenService;
import com.eprocure.iam.application.service.SessionData;
import com.eprocure.iam.application.service.SessionService;
import com.eprocure.iam.common.exception.BusinessException;
import com.eprocure.iam.common.exception.ErrorCode;
import com.eprocure.iam.domain.model.SessionRecord;
import com.eprocure.iam.domain.model.SortDirection;
import com.eprocure.iam.domain.model.User;
import com.eprocure.iam.domain.model.UserSearchCriteria;
import com.eprocure.iam.domain.model.UserSort;
import com.eprocure.iam.domain.model.UserStatus;
import com.eprocure.iam.domain.repository.Page;
import com.eprocure.iam.domain.repository.SessionRepository;
import com.eprocure.iam.domain.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LoginUseCaseTest {
    private static final UUID USER_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID DEPARTMENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void should_create_session_when_credentials_are_valid() {
        FakeUserRepository userRepository = new FakeUserRepository(activeUser());
        FakeSessionRepository sessionRepository = new FakeSessionRepository();
        FakeSessionCachePort sessionCachePort = new FakeSessionCachePort();
        SessionService sessionService = new SessionService(
                sessionRepository,
                userRepository,
                sessionCachePort,
                new OpaqueTokenService(),
                8);
        LoginUseCase useCase = new LoginUseCase(userRepository, (username, password) -> true, sessionService);

        LoginResult result = useCase.execute(new LoginCommand(
                "requester",
                "Password@123",
                UUID.randomUUID(),
                ClientContext.of("127.0.0.1", "JUnit")));

        assertThat(result.userId()).isEqualTo(USER_ID);
        assertThat(result.rawToken())
                .hasSize(64)
                .matches("^[a-f0-9]{64}$");
        assertThat(sessionRepository.revokeActiveByUserIdCalled).isTrue();
        assertThat(sessionRepository.savedSession).isNotNull();
        assertThat(sessionCachePort.storedSession).isNotNull();
        assertThat(userRepository.lastLoginAt).isNotNull();
    }

    @Test
    void should_throw_iam_001_when_credentials_are_invalid() {
        FakeUserRepository userRepository = new FakeUserRepository(activeUser());
        SessionService sessionService = new SessionService(
                new FakeSessionRepository(),
                userRepository,
                new FakeSessionCachePort(),
                new OpaqueTokenService(),
                8);
        CredentialVerificationPort credentialVerificationPort = (username, password) -> false;
        LoginUseCase useCase = new LoginUseCase(userRepository, credentialVerificationPort, sessionService);

        assertThatThrownBy(() -> useCase.execute(new LoginCommand(
                "requester",
                "wrong",
                UUID.randomUUID(),
                ClientContext.of("127.0.0.1", "JUnit"))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IAM_001);
    }

    private static User activeUser() {
        return User.create(
                USER_ID,
                "EMP-2025-00001",
                "requester",
                "requester@eprocure.local",
                "Request User",
                DEPARTMENT_ID,
                UserStatus.ACTIVE,
                Instant.parse("2026-05-17T00:00:00Z"));
    }

    private static final class FakeUserRepository implements UserRepository {
        private final User user;
        private Instant lastLoginAt;

        private FakeUserRepository(User user) {
            this.user = user;
        }

        @Override
        public Optional<User> findById(UUID id) {
            return USER_ID.equals(id) ? Optional.of(user) : Optional.empty();
        }

        @Override
        public Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
            return "requester".equals(usernameOrEmail) ? Optional.of(user) : Optional.empty();
        }

        @Override
        public Optional<User> findByEmployeeCodeOrUsernameOrEmail(String employeeCode, String username, String email) {
            return Optional.empty();
        }

        @Override
        public Page<User> findPage(UserSearchCriteria criteria, UserSort sort, SortDirection direction, int offset, int limit) {
            return new Page<>(List.of(user), 1);
        }

        @Override
        public Set<String> findRoleCodesByUserId(UUID userId) {
            return Set.of("REQUESTER");
        }

        @Override
        public Set<String> findPermissionCodesByUserId(UUID userId) {
            return Set.of("IAM_PROFILE_READ", "IAM_SESSION_REVOKE");
        }

        @Override
        public void save(User user, UUID actorId) {
        }

        @Override
        public void update(User user, UUID actorId) {
        }

        @Override
        public void updateStatus(UUID userId, UserStatus status, UUID actorId) {
        }

        @Override
        public void replaceRoles(UUID userId, Set<String> roleCodes, UUID actorId) {
        }

        @Override
        public void updateLastLoginAt(UUID userId, Instant lastLoginAt) {
            this.lastLoginAt = lastLoginAt;
        }
    }

    private static final class FakeSessionRepository implements SessionRepository {
        private boolean revokeActiveByUserIdCalled;
        private SessionRecord savedSession;

        @Override
        public void save(SessionRecord sessionRecord) {
            this.savedSession = sessionRecord;
        }

        @Override
        public void revokeActiveByUserId(UUID userId, UUID revokedBy, Instant revokedAt) {
            this.revokeActiveByUserIdCalled = true;
        }

        @Override
        public void revokeByTokenHash(String tokenHash, UUID revokedBy, Instant revokedAt) {
        }

        @Override
        public Optional<SessionRecord> findActiveByTokenHash(String tokenHash, Instant now) {
            return Optional.empty();
        }
    }

    private static final class FakeSessionCachePort implements SessionCachePort {
        private final Map<String, SessionData> sessions = new HashMap<>();
        private SessionData storedSession;

        @Override
        public void store(SessionData sessionData, Duration ttl) {
            this.storedSession = sessionData;
            sessions.put(sessionData.tokenHash(), sessionData);
        }

        @Override
        public Optional<SessionData> findByTokenHash(String tokenHash) {
            return Optional.ofNullable(sessions.get(tokenHash));
        }

        @Override
        public void evict(String tokenHash) {
            sessions.remove(tokenHash);
        }
    }
}
