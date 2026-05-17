package com.eprocure.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.eprocure.iam.application.port.in.ForgotPasswordCommand;
import com.eprocure.iam.application.port.out.PasswordResetDeliveryPort;
import com.eprocure.iam.application.service.IdempotencyGuard;
import com.eprocure.iam.application.service.OpaqueTokenService;
import com.eprocure.iam.domain.model.PasswordResetToken;
import com.eprocure.iam.domain.model.SortDirection;
import com.eprocure.iam.domain.model.User;
import com.eprocure.iam.domain.model.UserSearchCriteria;
import com.eprocure.iam.domain.model.UserSort;
import com.eprocure.iam.domain.model.UserStatus;
import com.eprocure.iam.domain.repository.Page;
import com.eprocure.iam.domain.repository.PasswordResetTokenRepository;
import com.eprocure.iam.domain.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ForgotPasswordUseCaseTest {
    private static final UUID USER_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID DEPARTMENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void should_create_reset_token_when_email_exists() {
        FakeUserRepository userRepository = new FakeUserRepository(Optional.of(activeUser()));
        FakePasswordResetTokenRepository tokenRepository = new FakePasswordResetTokenRepository();
        FakePasswordResetDeliveryPort deliveryPort = new FakePasswordResetDeliveryPort();
        ForgotPasswordUseCase useCase = new ForgotPasswordUseCase(
                userRepository,
                tokenRepository,
                deliveryPort,
                new OpaqueTokenService(),
                new IdempotencyGuard(),
                15);

        useCase.execute(new ForgotPasswordCommand("requester@eprocure.local", UUID.randomUUID().toString()));

        assertThat(tokenRepository.revokedUserId).isEqualTo(USER_ID);
        assertThat(tokenRepository.savedToken).isNotNull();
        assertThat(tokenRepository.savedToken.getUserId()).isEqualTo(USER_ID);
        assertThat(tokenRepository.savedToken.getTokenHash()).matches("^[a-f0-9]{64}$");
        assertThat(deliveryPort.rawToken).hasSize(64).matches("^[a-f0-9]{64}$");
        assertThat(deliveryPort.expiresAt).isEqualTo(tokenRepository.savedToken.getExpiresAt());
    }

    @Test
    void should_not_reveal_missing_email_when_user_does_not_exist() {
        FakeUserRepository userRepository = new FakeUserRepository(Optional.empty());
        FakePasswordResetTokenRepository tokenRepository = new FakePasswordResetTokenRepository();
        FakePasswordResetDeliveryPort deliveryPort = new FakePasswordResetDeliveryPort();
        ForgotPasswordUseCase useCase = new ForgotPasswordUseCase(
                userRepository,
                tokenRepository,
                deliveryPort,
                new OpaqueTokenService(),
                new IdempotencyGuard(),
                15);

        useCase.execute(new ForgotPasswordCommand("missing@eprocure.local", UUID.randomUUID().toString()));

        assertThat(tokenRepository.savedToken).isNull();
        assertThat(deliveryPort.rawToken).isNull();
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
        private UUID revokedUserId;
        private PasswordResetToken savedToken;

        @Override
        public void save(PasswordResetToken token, UUID actorId) {
            this.savedToken = token;
        }

        @Override
        public Optional<PasswordResetToken> findActiveByTokenHash(String tokenHash, Instant now) {
            return Optional.empty();
        }

        @Override
        public void markUsed(UUID tokenId, UUID actorId, Instant usedAt) {
        }

        @Override
        public void revokeActiveByUserId(UUID userId, UUID actorId, Instant revokedAt) {
            this.revokedUserId = userId;
        }
    }

    private static final class FakePasswordResetDeliveryPort implements PasswordResetDeliveryPort {
        private String rawToken;
        private Instant expiresAt;

        @Override
        public void sendResetInstructions(User user, String rawToken, Instant expiresAt) {
            this.rawToken = rawToken;
            this.expiresAt = expiresAt;
        }
    }

    private static final class FakeUserRepository implements UserRepository {
        private final Optional<User> user;

        private FakeUserRepository(Optional<User> user) {
            this.user = user;
        }

        @Override
        public Optional<User> findById(UUID id) {
            return user.filter(value -> value.getId().equals(id));
        }

        @Override
        public Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
            return user.filter(value -> value.getEmail().equals(usernameOrEmail));
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
}
