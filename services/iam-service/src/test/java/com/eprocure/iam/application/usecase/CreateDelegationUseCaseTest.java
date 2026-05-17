package com.eprocure.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eprocure.iam.application.port.in.CreateDelegationCommand;
import com.eprocure.iam.application.service.DelegationDetailView;
import com.eprocure.iam.application.service.DelegationViewAssembler;
import com.eprocure.iam.application.service.IdempotencyGuard;
import com.eprocure.iam.application.service.UserViewAssembler;
import com.eprocure.iam.common.exception.BusinessException;
import com.eprocure.iam.common.exception.ErrorCode;
import com.eprocure.iam.domain.model.Delegation;
import com.eprocure.iam.domain.model.DelegationScope;
import com.eprocure.iam.domain.model.DelegationStatus;
import com.eprocure.iam.domain.model.Department;
import com.eprocure.iam.domain.model.SortDirection;
import com.eprocure.iam.domain.model.User;
import com.eprocure.iam.domain.model.UserSearchCriteria;
import com.eprocure.iam.domain.model.UserSort;
import com.eprocure.iam.domain.model.UserStatus;
import com.eprocure.iam.domain.repository.DelegationRepository;
import com.eprocure.iam.domain.repository.DepartmentRepository;
import com.eprocure.iam.domain.repository.Page;
import com.eprocure.iam.domain.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreateDelegationUseCaseTest {
    private static final UUID DELEGATOR_ID = UUID.fromString("30000000-0000-0000-0000-000000000002");
    private static final UUID DELEGATE_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final UUID DEPARTMENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-17T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void should_create_delegation_when_delegate_is_same_or_higher_org_level() {
        FakeDelegationRepository delegationRepository = new FakeDelegationRepository("/HQ/PROCUREMENT", "/HQ");
        FakeUserRepository userRepository = new FakeUserRepository();
        CreateDelegationUseCase useCase = newUseCase(delegationRepository, userRepository);

        DelegationDetailView view = useCase.execute(new CreateDelegationCommand(
                DELEGATOR_ID,
                DELEGATE_ID,
                Instant.parse("2026-05-18T00:00:00Z"),
                Instant.parse("2026-05-20T00:00:00Z"),
                new BigDecimal("50000000.0000"),
                "VND",
                List.of("IT_EQUIPMENT"),
                DelegationScope.ALL), UUID.randomUUID().toString());

        assertThat(view.delegate().id()).isEqualTo(DELEGATE_ID);
        assertThat(view.maxValue()).isEqualTo("50000000.0000");
        assertThat(delegationRepository.savedDelegation).isNotNull();
    }

    @Test
    void should_throw_iam_020_when_delegate_is_lower_org_level() {
        FakeDelegationRepository delegationRepository = new FakeDelegationRepository("/HQ", "/HQ/PROCUREMENT");
        CreateDelegationUseCase useCase = newUseCase(delegationRepository, new FakeUserRepository());

        assertThatThrownBy(() -> useCase.execute(new CreateDelegationCommand(
                DELEGATOR_ID,
                DELEGATE_ID,
                Instant.parse("2026-05-18T00:00:00Z"),
                Instant.parse("2026-05-20T00:00:00Z"),
                null,
                "VND",
                null,
                DelegationScope.ALL), UUID.randomUUID().toString()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IAM_020);
    }

    private static CreateDelegationUseCase newUseCase(
            FakeDelegationRepository delegationRepository,
            FakeUserRepository userRepository) {
        UserViewAssembler userViewAssembler = new UserViewAssembler(new FakeDepartmentRepository(), userRepository);
        DelegationViewAssembler delegationViewAssembler = new DelegationViewAssembler(userRepository, userViewAssembler, CLOCK);
        return new CreateDelegationUseCase(
                delegationRepository,
                userRepository,
                delegationViewAssembler,
                new IdempotencyGuard(),
                CLOCK);
    }

    private static User activeUser(UUID id, String username) {
        return User.create(
                id,
                "EMP-" + username,
                username,
                username + "@eprocure.local",
                username,
                DEPARTMENT_ID,
                UserStatus.ACTIVE,
                Instant.parse("2026-05-17T00:00:00Z"));
    }

    private static final class FakeDelegationRepository implements DelegationRepository {
        private final String delegatorPath;
        private final String delegatePath;
        private Delegation savedDelegation;

        private FakeDelegationRepository(String delegatorPath, String delegatePath) {
            this.delegatorPath = delegatorPath;
            this.delegatePath = delegatePath;
        }

        @Override
        public Optional<Delegation> findById(UUID id) {
            return Optional.ofNullable(savedDelegation).filter(delegation -> delegation.getId().equals(id));
        }

        @Override
        public List<Delegation> findByDelegatorId(UUID delegatorId) {
            return savedDelegation == null ? List.of() : List.of(savedDelegation);
        }

        @Override
        public boolean hasActiveOverlap(UUID delegatorId, Instant startAt, Instant endAt) {
            return false;
        }

        @Override
        public Optional<String> findOrgPathByUserId(UUID userId) {
            if (DELEGATOR_ID.equals(userId)) {
                return Optional.of(delegatorPath);
            }
            if (DELEGATE_ID.equals(userId)) {
                return Optional.of(delegatePath);
            }
            return Optional.empty();
        }

        @Override
        public void save(Delegation delegation, UUID actorId) {
            this.savedDelegation = delegation;
        }

        @Override
        public void updateStatus(UUID delegationId, DelegationStatus status, UUID actorId) {
        }
    }

    private static final class FakeUserRepository implements UserRepository {
        private final Map<UUID, User> users = new HashMap<>();

        private FakeUserRepository() {
            users.put(DELEGATOR_ID, activeUser(DELEGATOR_ID, "manager"));
            users.put(DELEGATE_ID, activeUser(DELEGATE_ID, "director"));
        }

        @Override
        public Optional<User> findById(UUID id) {
            return Optional.ofNullable(users.get(id));
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
            return Set.of("MANAGER");
        }

        @Override
        public Set<String> findPermissionCodesByUserId(UUID userId) {
            return Set.of("DELEGATION_MANAGE");
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

    private static final class FakeDepartmentRepository implements DepartmentRepository {
        @Override
        public Optional<Department> findById(UUID id) {
            return Optional.of(Department.create(DEPARTMENT_ID, "PROCUREMENT", "Procurement", Instant.parse("2026-05-17T00:00:00Z")));
        }
    }
}
