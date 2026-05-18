package com.eprocure.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eprocure.iam.application.port.in.UpdateRolePermissionsCommand;
import com.eprocure.iam.application.service.IdempotencyGuard;
import com.eprocure.iam.domain.model.Role;
import com.eprocure.iam.domain.repository.PermissionRepository;
import com.eprocure.iam.domain.repository.RoleRepository;
import com.eprocure.iam.testsupport.StubPermissionResolutionService;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UpdateRolePermissionsUseCaseTest {
    private static final UUID ACTOR_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");

    @Test
    void should_refresh_role_permission_cache_when_role_permissions_change() {
        RoleRepository roleRepository = mock(RoleRepository.class);
        PermissionRepository permissionRepository = mock(PermissionRepository.class);
        StubPermissionResolutionService permissionResolutionService = new StubPermissionResolutionService();
        when(roleRepository.findByCode("MANAGER")).thenReturn(Optional.of(managerRole()));
        when(permissionRepository.findExistingCodes(Set.of("PR_APPROVE_L1"))).thenReturn(Set.of("PR_APPROVE_L1"));
        UpdateRolePermissionsUseCase useCase = new UpdateRolePermissionsUseCase(
                roleRepository,
                permissionRepository,
                new IdempotencyGuard(),
                permissionResolutionService);

        useCase.execute(
                new UpdateRolePermissionsCommand(ACTOR_ID, "MANAGER", java.util.List.of("pr_approve_l1")),
                UUID.randomUUID().toString());

        verify(roleRepository).replacePermissions("MANAGER", Set.of("PR_APPROVE_L1"), ACTOR_ID);
        assertThat(permissionResolutionService.refreshedRoleCode()).isEqualTo("MANAGER");
        assertThat(permissionResolutionService.refreshedPermissionCodes()).containsExactly("PR_APPROVE_L1");
    }

    private static Role managerRole() {
        return Role.create(
                UUID.fromString("50000000-0000-0000-0000-000000000001"),
                "MANAGER",
                "Manager",
                null,
                false,
                Instant.parse("2026-05-17T00:00:00Z"));
    }
}
