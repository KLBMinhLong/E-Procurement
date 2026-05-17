package com.eprocure.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.eprocure.iam.application.service.PasswordHashService;
import com.eprocure.iam.domain.model.SortDirection;
import com.eprocure.iam.domain.model.User;
import com.eprocure.iam.domain.model.UserSearchCriteria;
import com.eprocure.iam.domain.model.UserSort;
import com.eprocure.iam.domain.model.UserStatus;
import com.eprocure.iam.domain.repository.Page;
import com.eprocure.iam.domain.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VerifyKeycloakCredentialUseCaseTest {
    private static final UUID USER_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID DEPARTMENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void should_return_true_when_password_matches_iam_hash() {
        PasswordHashService passwordHashService = new PasswordHashService();
        FakeUserRepository userRepository = new FakeUserRepository(
                activeUser(),
                passwordHashService.hash("Password@123", USER_ID));
        VerifyKeycloakCredentialUseCase useCase = new VerifyKeycloakCredentialUseCase(userRepository, passwordHashService);

        boolean valid = useCase.execute("requester", "Password@123");

        assertThat(valid).isTrue();
    }

    @Test
    void should_return_false_when_hash_is_missing() {
        PasswordHashService passwordHashService = new PasswordHashService();
        FakeUserRepository userRepository = new FakeUserRepository(activeUser(), "");
        VerifyKeycloakCredentialUseCase useCase = new VerifyKeycloakCredentialUseCase(userRepository, passwordHashService);

        assertThat(useCase.execute("requester", "Password@123")).isFalse();
    }

    @Test
    void should_return_false_when_password_does_not_match_hash() {
        PasswordHashService passwordHashService = new PasswordHashService();
        FakeUserRepository userRepository = new FakeUserRepository(
                activeUser(),
                passwordHashService.hash("Password@123", USER_ID));
        VerifyKeycloakCredentialUseCase useCase = new VerifyKeycloakCredentialUseCase(userRepository, passwordHashService);

        assertThat(useCase.execute("requester", "WrongPassword@123")).isFalse();
    }

    @Test
    void should_return_false_when_user_cannot_login() {
        PasswordHashService passwordHashService = new PasswordHashService();
        User lockedUser = User.create(
                USER_ID,
                "EMP-2025-00001",
                "requester",
                "requester@eprocure.local",
                "Request User",
                DEPARTMENT_ID,
                UserStatus.LOCKED,
                Instant.parse("2026-05-17T00:00:00Z"));
        FakeUserRepository userRepository = new FakeUserRepository(
                lockedUser,
                passwordHashService.hash("Password@123", USER_ID));
        VerifyKeycloakCredentialUseCase useCase = new VerifyKeycloakCredentialUseCase(userRepository, passwordHashService);

        assertThat(useCase.execute("requester", "Password@123")).isFalse();
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
        private final String passwordHash;

        private FakeUserRepository(User user, String passwordHash) {
            this.user = user;
            this.passwordHash = passwordHash;
        }

        @Override
        public Optional<User> findById(UUID id) {
            return user.getId().equals(id) ? Optional.of(user) : Optional.empty();
        }

        @Override
        public Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
            return user.getUsername().equals(usernameOrEmail) || user.getEmail().equals(usernameOrEmail)
                    ? Optional.of(user)
                    : Optional.empty();
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
            return Optional.ofNullable(passwordHash).filter(value -> !value.isBlank());
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
