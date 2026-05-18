package com.eprocure.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eprocure.iam.application.port.in.ResetPasswordCommand;
import com.eprocure.iam.application.service.IdempotencyGuard;
import com.eprocure.iam.application.service.OpaqueTokenService;
import com.eprocure.iam.application.service.PasswordHashService;
import com.eprocure.iam.application.service.PasswordPolicyService;
import com.eprocure.iam.application.service.SessionData;
import com.eprocure.iam.application.service.SessionService;
import com.eprocure.iam.application.port.out.SessionCachePort;
import com.eprocure.iam.common.exception.BusinessException;
import com.eprocure.iam.common.exception.ErrorCode;
import com.eprocure.iam.testsupport.StubPermissionResolutionService;
import com.eprocure.iam.domain.model.PasswordResetToken;
import com.eprocure.iam.domain.model.SessionRecord;
import com.eprocure.iam.domain.model.SortDirection;
import com.eprocure.iam.domain.model.User;
import com.eprocure.iam.domain.model.UserSearchCriteria;
import com.eprocure.iam.domain.model.UserSort;
import com.eprocure.iam.domain.model.UserStatus;
import com.eprocure.iam.domain.repository.Page;
import com.eprocure.iam.domain.repository.PasswordHistoryRepository;
import com.eprocure.iam.domain.repository.PasswordResetTokenRepository;
import com.eprocure.iam.domain.repository.SessionRepository;
import com.eprocure.iam.domain.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ResetPasswordUseCaseTest {
    private static final UUID USER_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID TOKEN_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final UUID DEPARTMENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String RAW_TOKEN = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Test
    void should_reset_password_and_revoke_sessions_when_token_is_valid() {
        OpaqueTokenService opaqueTokenService = new OpaqueTokenService();
        PasswordHashService passwordHashService = new PasswordHashService();
        FakePasswordResetTokenRepository tokenRepository = new FakePasswordResetTokenRepository(validToken(opaqueTokenService));
        FakePasswordHistoryRepository passwordHistoryRepository = new FakePasswordHistoryRepository();
        FakeUserRepository userRepository = new FakeUserRepository(activeUser());
        FakeSessionRepository sessionRepository = new FakeSessionRepository(List.of("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"));
        FakeSessionCachePort sessionCachePort = new FakeSessionCachePort();
        ResetPasswordUseCase useCase = new ResetPasswordUseCase(
                tokenRepository,
                passwordHistoryRepository,
                userRepository,
                new SessionService(
                        sessionRepository,
                        userRepository,
                        sessionCachePort,
                        opaqueTokenService,
                        new StubPermissionResolutionService(),
                        8),
                opaqueTokenService,
                passwordHashService,
                new PasswordPolicyService(passwordHashService),
                new IdempotencyGuard());

        useCase.execute(new ResetPasswordCommand(
                RAW_TOKEN,
                "BetterPass123!",
                "BetterPass123!",
                UUID.randomUUID().toString()));

        assertThat(userRepository.updatedPasswordHash).startsWith("$2a$12$");
        assertThat(passwordHistoryRepository.savedPasswordHash).isEqualTo(userRepository.updatedPasswordHash);
        assertThat(tokenRepository.usedTokenId).isEqualTo(TOKEN_ID);
        assertThat(tokenRepository.revokedUserId).isEqualTo(USER_ID);
        assertThat(sessionRepository.revokeActiveByUserIdCalled).isTrue();
        assertThat(sessionCachePort.evictedTokenHashes).containsExactly("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
    }

    @Test
    void should_throw_iam_007_when_reset_token_is_invalid() {
        ResetPasswordUseCase useCase = useCaseWithToken(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new ResetPasswordCommand(
                RAW_TOKEN,
                "BetterPass123!",
                "BetterPass123!",
                UUID.randomUUID().toString())))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IAM_007);
    }

    @Test
    void should_throw_iam_008_when_new_password_is_weak() {
        OpaqueTokenService opaqueTokenService = new OpaqueTokenService();
        ResetPasswordUseCase useCase = useCaseWithToken(Optional.of(validToken(opaqueTokenService)));

        assertThatThrownBy(() -> useCase.execute(new ResetPasswordCommand(
                RAW_TOKEN,
                "weakpass",
                "weakpass",
                UUID.randomUUID().toString())))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IAM_008);
    }

    private static ResetPasswordUseCase useCaseWithToken(Optional<PasswordResetToken> token) {
        OpaqueTokenService opaqueTokenService = new OpaqueTokenService();
        PasswordHashService passwordHashService = new PasswordHashService();
        FakeUserRepository userRepository = new FakeUserRepository(activeUser());
        return new ResetPasswordUseCase(
                new FakePasswordResetTokenRepository(token.orElse(null)),
                new FakePasswordHistoryRepository(),
                userRepository,
                new SessionService(
                        new FakeSessionRepository(List.of()),
                        userRepository,
                        new FakeSessionCachePort(),
                        opaqueTokenService,
                        new StubPermissionResolutionService(),
                        8),
                opaqueTokenService,
                passwordHashService,
                new PasswordPolicyService(passwordHashService),
                new IdempotencyGuard());
    }

    private static PasswordResetToken validToken(OpaqueTokenService opaqueTokenService) {
        return PasswordResetToken.issue(
                TOKEN_ID,
                USER_ID,
                opaqueTokenService.hash(RAW_TOKEN),
                Instant.now().plus(Duration.ofMinutes(15)),
                Instant.now());
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

    private static final class FakePasswordResetTokenRepository implements PasswordResetTokenRepository {
        private final PasswordResetToken token;
        private UUID usedTokenId;
        private UUID revokedUserId;

        private FakePasswordResetTokenRepository(PasswordResetToken token) {
            this.token = token;
        }

        @Override
        public void save(PasswordResetToken token, UUID actorId) {
        }

        @Override
        public Optional<PasswordResetToken> findActiveByTokenHash(String tokenHash, Instant now) {
            return Optional.ofNullable(token).filter(value -> value.getTokenHash().equals(tokenHash));
        }

        @Override
        public void markUsed(UUID tokenId, UUID actorId, Instant usedAt) {
            this.usedTokenId = tokenId;
        }

        @Override
        public void revokeActiveByUserId(UUID userId, UUID actorId, Instant revokedAt) {
            this.revokedUserId = userId;
        }
    }

    private static final class FakePasswordHistoryRepository implements PasswordHistoryRepository {
        private String savedPasswordHash;

        @Override
        public List<String> findRecentHashesByUserId(UUID userId, int limit) {
            return List.of();
        }

        @Override
        public void save(UUID userId, String passwordHash, UUID actorId, Instant changedAt) {
            this.savedPasswordHash = passwordHash;
        }
    }

    private static final class FakeUserRepository implements UserRepository {
        private final User user;
        private String updatedPasswordHash;

        private FakeUserRepository(User user) {
            this.user = user;
        }

        @Override
        public Optional<User> findById(UUID id) {
            return user.getId().equals(id) ? Optional.of(user) : Optional.empty();
        }

        @Override
        public Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
            return Optional.empty();
        }

        @Override
        public Optional<User> findByGoogleOauthId(String googleOauthId) {
            return Optional.empty();
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
            return new Page<>(List.of(), 0);
        }

        @Override
        public Set<String> findRoleCodesByUserId(UUID userId) {
            return Set.of();
        }

        @Override
        public Set<String> findPermissionCodesByUserId(UUID userId) {
            return Set.of();
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
        }

        @Override
        public void updatePasswordHash(UUID userId, String passwordHash, UUID actorId) {
            this.updatedPasswordHash = passwordHash;
        }

        @Override
        public void linkGoogleOauthId(UUID userId, String googleOauthId, UUID actorId) {
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
        private final List<String> activeTokenHashes;
        private boolean revokeActiveByUserIdCalled;

        private FakeSessionRepository(List<String> activeTokenHashes) {
            this.activeTokenHashes = activeTokenHashes;
        }

        @Override
        public void save(SessionRecord sessionRecord) {
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

        @Override
        public List<String> findActiveTokenHashesByUserId(UUID userId, Instant now) {
            return activeTokenHashes;
        }
    }

    private static final class FakeSessionCachePort implements SessionCachePort {
        private final List<String> evictedTokenHashes = new ArrayList<>();

        @Override
        public void store(SessionData sessionData, Duration ttl) {
        }

        @Override
        public Optional<SessionData> findByTokenHash(String tokenHash) {
            return Optional.empty();
        }

        @Override
        public void evict(String tokenHash) {
            evictedTokenHashes.add(tokenHash);
        }
    }
}
