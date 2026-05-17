package com.eprocure.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eprocure.iam.application.port.in.ResolveApproversQuery;
import com.eprocure.iam.application.service.UserSummaryView;
import com.eprocure.iam.application.service.UserViewAssembler;
import com.eprocure.iam.common.exception.BusinessException;
import com.eprocure.iam.common.exception.ErrorCode;
import com.eprocure.iam.domain.model.Department;
import com.eprocure.iam.domain.model.Role;
import com.eprocure.iam.domain.model.SortDirection;
import com.eprocure.iam.domain.model.User;
import com.eprocure.iam.domain.model.UserSearchCriteria;
import com.eprocure.iam.domain.model.UserSort;
import com.eprocure.iam.domain.model.UserStatus;
import com.eprocure.iam.domain.repository.DepartmentRepository;
import com.eprocure.iam.domain.repository.OrganizationRepository;
import com.eprocure.iam.domain.repository.Page;
import com.eprocure.iam.domain.repository.RoleRepository;
import com.eprocure.iam.domain.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ResolveApproversUseCaseTest {
    private static final UUID DEPARTMENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID APPROVER_ID = UUID.fromString("30000000-0000-0000-0000-000000000002");
    private static final UUID REQUESTER_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");

    @Test
    void should_return_approvers_when_role_exists() {
        FakeOrganizationRepository organizationRepository = new FakeOrganizationRepository(List.of(user(APPROVER_ID, "manager")));
        ResolveApproversUseCase useCase = newUseCase(organizationRepository, new FakeRoleRepository(Set.of("MANAGER")));

        List<UserSummaryView> approvers = useCase.execute(new ResolveApproversQuery("MANAGER", DEPARTMENT_ID, REQUESTER_ID));

        assertThat(approvers).hasSize(1);
        assertThat(approvers.get(0).id()).isEqualTo(APPROVER_ID);
        assertThat(organizationRepository.excludedUserId).isEqualTo(REQUESTER_ID);
    }

    @Test
    void should_throw_iam_034_when_no_approver_found() {
        ResolveApproversUseCase useCase = newUseCase(
                new FakeOrganizationRepository(List.of()),
                new FakeRoleRepository(Set.of("MANAGER")));

        assertThatThrownBy(() -> useCase.execute(new ResolveApproversQuery("MANAGER", DEPARTMENT_ID, REQUESTER_ID)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IAM_034);
    }

    private static ResolveApproversUseCase newUseCase(
            FakeOrganizationRepository organizationRepository,
            FakeRoleRepository roleRepository) {
        UserRepository userRepository = new EmptyUserRepository();
        UserViewAssembler assembler = new UserViewAssembler(new FakeDepartmentRepository(), userRepository);
        return new ResolveApproversUseCase(organizationRepository, roleRepository, assembler);
    }

    private static User user(UUID id, String username) {
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

    private static final class FakeOrganizationRepository implements OrganizationRepository {
        private final List<User> approvers;
        private UUID excludedUserId;

        private FakeOrganizationRepository(List<User> approvers) {
            this.approvers = approvers;
        }

        @Override
        public List<Department> findAllDepartments() {
            return List.of();
        }

        @Override
        public Optional<Department> findDepartmentById(UUID departmentId) {
            return Optional.of(Department.create(DEPARTMENT_ID, "PROCUREMENT", "Procurement", Instant.parse("2026-05-17T00:00:00Z")));
        }

        @Override
        public long countActiveMembersByDepartmentId(UUID departmentId) {
            return 0;
        }

        @Override
        public Page<User> findDepartmentMembers(UUID departmentId, int offset, int limit) {
            return new Page<>(List.of(), 0);
        }

        @Override
        public List<User> findApprovers(String roleCode, UUID departmentId, UUID excludedUserId, int limit) {
            this.excludedUserId = excludedUserId;
            return approvers;
        }
    }

    private static final class FakeRoleRepository implements RoleRepository {
        private final Set<String> existingCodes;

        private FakeRoleRepository(Set<String> existingCodes) {
            this.existingCodes = existingCodes;
        }

        @Override
        public List<Role> findAll() {
            return List.of();
        }

        @Override
        public Optional<Role> findByCode(String code) {
            return Optional.empty();
        }

        @Override
        public Set<String> findExistingCodes(Set<String> codes) {
            return existingCodes;
        }

        @Override
        public Set<String> findPermissionCodesByRoleCode(String code) {
            return Set.of();
        }

        @Override
        public void save(Role role, Set<String> permissionCodes, UUID actorId) {
        }

        @Override
        public void replacePermissions(String roleCode, Set<String> permissionCodes, UUID actorId) {
        }
    }

    private static final class FakeDepartmentRepository implements DepartmentRepository {
        @Override
        public Optional<Department> findById(UUID id) {
            return Optional.of(Department.create(DEPARTMENT_ID, "PROCUREMENT", "Procurement", Instant.parse("2026-05-17T00:00:00Z")));
        }
    }

    private static final class EmptyUserRepository implements UserRepository {
        @Override
        public Optional<User> findById(UUID id) {
            return Optional.empty();
        }

        @Override
        public Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
            return Optional.empty();
        }

        @Override
        public Optional<User> findByEmployeeCodeOrUsernameOrEmail(String employeeCode, String username, String email) {
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
