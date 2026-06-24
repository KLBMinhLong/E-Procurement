package com.eprocure.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.eprocure.iam.application.port.in.ResolveActiveDelegationQuery;
import com.eprocure.iam.application.service.ActiveDelegationView;
import com.eprocure.iam.application.service.UserViewAssembler;
import com.eprocure.iam.domain.model.Delegation;
import com.eprocure.iam.domain.model.DelegationScope;
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
import com.eprocure.iam.testsupport.StubPermissionResolutionService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ResolveActiveDelegationUseCaseTest {
    private static final UUID DELEGATION_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final UUID DELEGATOR_ID = UUID.fromString("30000000-0000-0000-0000-000000000002");
    private static final UUID DELEGATE_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final UUID REQUESTER_ID = UUID.fromString("30000000-0000-0000-0000-000000000004");
    private static final UUID DEPARTMENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-28T02:00:00Z"), ZoneOffset.UTC);

    @Test
    void should_return_active_delegation_when_matching_delegation_and_delegate_exist() {
        FakeDelegationRepository delegationRepository = new FakeDelegationRepository(Optional.of(activeDelegation()));
        ResolveActiveDelegationUseCase useCase = newUseCase(delegationRepository, new FakeUserRepository(true));

        ActiveDelegationView result = useCase.execute(defaultQuery());

        assertThat(result.active()).isTrue();
        assertThat(result.delegationId()).isEqualTo(DELEGATION_ID);
        assertThat(result.delegatorId()).isEqualTo(DELEGATOR_ID);
        assertThat(result.delegateId()).isEqualTo(DELEGATE_ID);
        assertThat(result.delegate().id()).isEqualTo(DELEGATE_ID);
        assertThat(delegationRepository.effectiveAt).isEqualTo(CLOCK.instant());
        assertThat(delegationRepository.categories).containsExactly("IT_EQUIPMENT");
    }

    @Test
    void should_return_inactive_when_delegate_user_no_longer_exists() {
        ResolveActiveDelegationUseCase useCase = newUseCase(
                new FakeDelegationRepository(Optional.of(activeDelegation())),
                new FakeUserRepository(false));

        ActiveDelegationView result = useCase.execute(defaultQuery());

        assertThat(result.active()).isFalse();
    }

    private static ResolveActiveDelegationUseCase newUseCase(
            FakeDelegationRepository delegationRepository,
            FakeUserRepository userRepository) {
        UserViewAssembler assembler = new UserViewAssembler(
                new FakeDepartmentRepository(),
                userRepository,
                new StubPermissionResolutionService());
        return new ResolveActiveDelegationUseCase(delegationRepository, userRepository, assembler, CLOCK);
    }

    private static ResolveActiveDelegationQuery defaultQuery() {
        return new ResolveActiveDelegationQuery(
                DELEGATOR_ID,
                REQUESTER_ID,
                DEPARTMENT_ID,
                new BigDecimal("25000000.0000"),
                "vnd",
                List.of("it_equipment"));
    }

    private static Delegation activeDelegation() {
        return Delegation.create(
                DELEGATION_ID,
                DELEGATOR_ID,
                DELEGATE_ID,
                Instant.parse("2026-05-27T00:00:00Z"),
                Instant.parse("2026-05-29T00:00:00Z"),
                new BigDecimal("50000000.0000"),
                "VND",
                List.of("IT_EQUIPMENT"),
                DelegationScope.ALL,
                Instant.parse("2026-05-27T00:00:00Z"));
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
        private final Optional<Delegation> delegation;
        private List<String> categories = List.of();
        private Instant effectiveAt;

        private FakeDelegationRepository(Optional<Delegation> delegation) {
            this.delegation = delegation;
        }

        @Override
        public Optional<Delegation> findById(UUID id) {
            return Optional.empty();
        }

        @Override
        public List<Delegation> findByDelegatorId(UUID delegatorId) {
            return List.of();
        }

        @Override
        public Optional<Delegation> findActiveForApproval(
                UUID delegatorId,
                UUID requesterDepartmentId,
                BigDecimal totalAmount,
                String currency,
                List<String> categories,
                Instant effectiveAt) {
            this.categories = categories;
            this.effectiveAt = effectiveAt;
            return delegation;
        }

        @Override
        public boolean hasActiveOverlap(UUID delegatorId, Instant startAt, Instant endAt) {
            return false;
        }

        @Override
        public Optional<String> findOrgPathByUserId(UUID userId) {
            return Optional.empty();
        }

        @Override
        public void save(Delegation delegation, UUID actorId) {
        }

        @Override
        public void updateStatus(UUID delegationId, com.eprocure.iam.domain.model.DelegationStatus status, UUID actorId) {
        }
    }

    private static final class FakeUserRepository implements UserRepository {
        private final boolean includeDelegate;

        private FakeUserRepository(boolean includeDelegate) {
            this.includeDelegate = includeDelegate;
        }

        @Override
        public Optional<User> findById(UUID id) {
            if (includeDelegate && DELEGATE_ID.equals(id)) {
                return Optional.of(activeUser(DELEGATE_ID, "director"));
            }
            return Optional.empty();
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
            return Set.of("PR_APPROVE_L1");
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
    }

    private static final class FakeDepartmentRepository implements DepartmentRepository {
        @Override
        public Optional<Department> findById(UUID id) {
            return Optional.of(Department.create(DEPARTMENT_ID, "PROCUREMENT", "Procurement", Instant.parse("2026-05-17T00:00:00Z")));
        }

        @Override
        public Optional<Department> findByIdIncludingInactive(UUID id) {
            return findById(id);
        }

        @Override
        public Optional<Department> findByCode(String code) {
            return Optional.empty();
        }

        @Override
        public boolean existsActiveByCode(String code) {
            return false;
        }

        @Override
        public boolean existsActiveByCodeExceptId(String code, UUID excludedId) {
            return false;
        }

        @Override
        public boolean isDescendant(UUID candidateParentId, UUID departmentId) {
            return false;
        }

        @Override
        public long countActiveMembersByDepartmentId(UUID departmentId) {
            return 0;
        }

        @Override
        public long countActiveChildrenByDepartmentId(UUID departmentId) {
            return 0;
        }

        @Override
        public void save(Department department, UUID actorId) {
        }

        @Override
        public void update(Department department, UUID actorId) {
        }

        @Override
        public void deactivate(UUID departmentId, UUID actorId, Instant deletedAt) {
        }
    }
}
