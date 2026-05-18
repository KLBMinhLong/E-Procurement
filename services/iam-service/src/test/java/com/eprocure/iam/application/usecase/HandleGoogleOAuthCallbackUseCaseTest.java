package com.eprocure.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eprocure.iam.application.port.in.ClientContext;
import com.eprocure.iam.application.port.in.GoogleOAuthCallbackCommand;
import com.eprocure.iam.application.port.out.GoogleOAuthPort;
import com.eprocure.iam.application.port.out.GoogleOAuthProfile;
import com.eprocure.iam.application.port.out.OAuthStateCachePort;
import com.eprocure.iam.application.port.out.SessionCachePort;
import com.eprocure.iam.application.port.out.TwoFactorChallengeCachePort;
import com.eprocure.iam.application.service.LoginResult;
import com.eprocure.iam.application.service.OAuthStateService;
import com.eprocure.iam.application.service.OpaqueTokenService;
import com.eprocure.iam.application.service.SessionData;
import com.eprocure.iam.application.service.SessionService;
import com.eprocure.iam.application.service.TwoFactorChallengeData;
import com.eprocure.iam.application.service.TwoFactorChallengeService;
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
import com.eprocure.iam.testsupport.StubPermissionResolutionService;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HandleGoogleOAuthCallbackUseCaseTest {
    private static final UUID USER_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID DEPARTMENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String STATE = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    @Test
    void should_link_google_subject_and_issue_session_when_profile_email_matches() {
        OpaqueTokenService opaqueTokenService = new OpaqueTokenService();
        FakeOAuthStateCachePort stateCachePort = new FakeOAuthStateCachePort();
        stateCachePort.store(opaqueTokenService.hash(STATE), Duration.ofMinutes(5));
        FakeUserRepository userRepository = new FakeUserRepository(activeUser());
        FakeSessionRepository sessionRepository = new FakeSessionRepository();
        FakeSessionCachePort sessionCachePort = new FakeSessionCachePort();
        HandleGoogleOAuthCallbackUseCase useCase = newUseCase(
                new FakeGoogleOAuthPort(new GoogleOAuthProfile(
                        "google-sub-1",
                        "requester@eprocure.local",
                        true,
                        "Request User",
                        null)),
                new OAuthStateService(stateCachePort, opaqueTokenService, 5),
                userRepository,
                sessionRepository,
                sessionCachePort);

        LoginResult result = useCase.execute(new GoogleOAuthCallbackCommand(
                "google-auth-code",
                STATE,
                STATE,
                ClientContext.of("127.0.0.1", "JUnit")));

        assertThat(result.userId()).isEqualTo(USER_ID);
        assertThat(result.requiresTwoFactor()).isFalse();
        assertThat(result.rawToken()).hasSize(64).matches("^[a-f0-9]{64}$");
        assertThat(userRepository.linkedGoogleOauthId).isEqualTo("google-sub-1");
        assertThat(sessionRepository.savedSession).isNotNull();
        assertThat(sessionCachePort.storedSession).isNotNull();
        assertThat(userRepository.lastLoginAt).isNotNull();
    }

    @Test
    void should_throw_iam_012_when_state_is_invalid() {
        HandleGoogleOAuthCallbackUseCase useCase = newUseCase(
                new FakeGoogleOAuthPort(new GoogleOAuthProfile(
                        "google-sub-1",
                        "requester@eprocure.local",
                        true,
                        "Request User",
                        null)),
                new OAuthStateService(new FakeOAuthStateCachePort(), new OpaqueTokenService(), 5),
                new FakeUserRepository(activeUser()),
                new FakeSessionRepository(),
                new FakeSessionCachePort());

        assertThatThrownBy(() -> useCase.execute(new GoogleOAuthCallbackCommand(
                "google-auth-code",
                STATE,
                STATE,
                ClientContext.of("127.0.0.1", "JUnit"))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IAM_012);
    }

    private static HandleGoogleOAuthCallbackUseCase newUseCase(
            GoogleOAuthPort googleOAuthPort,
            OAuthStateService oauthStateService,
            FakeUserRepository userRepository,
            FakeSessionRepository sessionRepository,
            FakeSessionCachePort sessionCachePort) {
        return new HandleGoogleOAuthCallbackUseCase(
                googleOAuthPort,
                oauthStateService,
                userRepository,
                new SessionService(
                        sessionRepository,
                        userRepository,
                        sessionCachePort,
                        new OpaqueTokenService(),
                        new StubPermissionResolutionService(),
                        8),
                new TwoFactorChallengeService(new FakeTwoFactorChallengeCachePort(), new OpaqueTokenService(), 5));
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

    private record FakeGoogleOAuthPort(GoogleOAuthProfile profile) implements GoogleOAuthPort {
        @Override
        public String authorizationUrl(String state) {
            return "https://accounts.google.com/o/oauth2/v2/auth?state=" + state;
        }

        @Override
        public Optional<GoogleOAuthProfile> fetchProfile(String authorizationCode) {
            return Optional.of(profile);
        }
    }

    private static final class FakeOAuthStateCachePort implements OAuthStateCachePort {
        private final Set<String> states = new java.util.HashSet<>();

        @Override
        public void store(String stateHash, Duration ttl) {
            states.add(stateHash);
        }

        @Override
        public boolean exists(String stateHash) {
            return states.contains(stateHash);
        }

        @Override
        public void evict(String stateHash) {
            states.remove(stateHash);
        }
    }

    private static final class FakeUserRepository implements UserRepository {
        private final User user;
        private String linkedGoogleOauthId;
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
            return "requester@eprocure.local".equals(usernameOrEmail) ? Optional.of(user) : Optional.empty();
        }

        @Override
        public Optional<User> findByGoogleOauthId(String googleOauthId) {
            return googleOauthId.equals(linkedGoogleOauthId) ? Optional.of(user) : Optional.empty();
        }

        @Override
        public Optional<User> findByEmployeeCodeOrUsernameOrEmail(String employeeCode, String username, String email) {
            return Optional.empty();
        }

        @Override
        public Optional<String> findPasswordHashById(UUID userId) {
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

        @Override
        public void updatePasswordHash(UUID userId, String passwordHash, UUID actorId) {
        }

        @Override
        public void linkGoogleOauthId(UUID userId, String googleOauthId, UUID actorId) {
            this.linkedGoogleOauthId = googleOauthId;
        }

        @Override
        public void stageTwoFactorSecret(UUID userId, String encryptedSecret, UUID actorId) {
        }

        @Override
        public void confirmTwoFactor(
                UUID userId,
                String encryptedSecret,
                List<String> backupCodeHashes,
                Instant confirmedAt,
                UUID actorId) {
        }
    }

    private static final class FakeSessionRepository implements SessionRepository {
        private SessionRecord savedSession;

        @Override
        public void save(SessionRecord sessionRecord) {
            this.savedSession = sessionRecord;
        }

        @Override
        public void revokeActiveByUserId(UUID userId, UUID revokedBy, Instant revokedAt) {
        }

        @Override
        public void revokeByTokenHash(String tokenHash, UUID revokedBy, Instant revokedAt) {
        }

        @Override
        public Optional<SessionRecord> findActiveByTokenHash(String tokenHash, Instant now) {
            return Optional.empty();
        }

        @Override
        public List<String> findActiveTokenHashesByUserId(UUID userId, Instant now) {
            return List.of();
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

    private static final class FakeTwoFactorChallengeCachePort implements TwoFactorChallengeCachePort {
        @Override
        public void store(TwoFactorChallengeData challengeData, Duration ttl) {
        }

        @Override
        public Optional<TwoFactorChallengeData> findByTokenHash(String tokenHash) {
            return Optional.empty();
        }

        @Override
        public void evict(String tokenHash) {
        }
    }
}
