package com.eprocure.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eprocure.iam.application.port.in.CreateRoleCommand;
import com.eprocure.iam.application.service.IdempotencyGuard;
import com.eprocure.iam.application.service.RoleDetailView;
import com.eprocure.iam.common.exception.BusinessException;
import com.eprocure.iam.common.exception.ErrorCode;
import com.eprocure.iam.domain.model.Permission;
import com.eprocure.iam.domain.model.Role;
import com.eprocure.iam.domain.repository.PermissionRepository;
import com.eprocure.iam.domain.repository.RoleRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import com.eprocure.iam.testsupport.StubPermissionResolutionService;
import org.junit.jupiter.api.Test;

class CreateRoleUseCaseTest {
    @Test
    void should_create_role_when_permissions_exist() {
        FakeRoleRepository roleRepository = new FakeRoleRepository();
        StubPermissionResolutionService permissionResolutionService = new StubPermissionResolutionService();
        CreateRoleUseCase useCase = new CreateRoleUseCase(
                roleRepository,
                new FakePermissionRepository(Set.of("ADMIN_USER_VIEW")),
                new IdempotencyGuard(),
                permissionResolutionService);

        RoleDetailView view = useCase.execute(
                new CreateRoleCommand(UUID.randomUUID(), "AUDITOR", "Auditor", null, List.of("ADMIN_USER_VIEW")),
                UUID.randomUUID().toString());

        assertThat(view.code()).isEqualTo("AUDITOR");
        assertThat(view.permissions()).containsExactly("ADMIN_USER_VIEW");
        assertThat(roleRepository.savedRole).isNotNull();
        assertThat(permissionResolutionService.refreshedRoleCode()).isEqualTo("AUDITOR");
        assertThat(permissionResolutionService.refreshedPermissionCodes()).containsExactly("ADMIN_USER_VIEW");
    }

    @Test
    void should_throw_iam_032_when_permission_missing() {
        CreateRoleUseCase useCase = new CreateRoleUseCase(
                new FakeRoleRepository(),
                new FakePermissionRepository(Set.of()),
                new IdempotencyGuard(),
                new StubPermissionResolutionService());

        assertThatThrownBy(() -> useCase.execute(
                new CreateRoleCommand(UUID.randomUUID(), "AUDITOR", "Auditor", null, List.of("ADMIN_USER_VIEW")),
                UUID.randomUUID().toString()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IAM_032);
    }

    private static final class FakeRoleRepository implements RoleRepository {
        private Role savedRole;
        private final Set<String> permissions = new HashSet<>();

        @Override
        public List<Role> findAll() {
            return savedRole == null ? List.of() : List.of(savedRole);
        }

        @Override
        public Optional<Role> findByCode(String code) {
            return Optional.empty();
        }

        @Override
        public Set<String> findExistingCodes(Set<String> codes) {
            return Set.of();
        }

        @Override
        public Set<String> findPermissionCodesByRoleCode(String code) {
            return Set.copyOf(permissions);
        }

        @Override
        public void save(Role role, Set<String> permissionCodes, UUID actorId) {
            this.savedRole = role;
            this.permissions.clear();
            this.permissions.addAll(permissionCodes);
        }

        @Override
        public void update(Role role, UUID actorId) {
        }

        @Override
        public void replacePermissions(String roleCode, Set<String> permissionCodes, UUID actorId) {
            this.permissions.clear();
            this.permissions.addAll(permissionCodes);
        }
    }

    private static final class FakePermissionRepository implements PermissionRepository {
        private final Set<String> existingCodes;

        private FakePermissionRepository(Set<String> existingCodes) {
            this.existingCodes = existingCodes;
        }

        @Override
        public List<Permission> findAll() {
            return new ArrayList<>();
        }

        @Override
        public Set<String> findExistingCodes(Set<String> codes) {
            return existingCodes;
        }
    }
}
