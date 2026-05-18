package com.eprocure.iam.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eprocure.iam.application.port.out.PermissionCachePort;
import com.eprocure.iam.domain.repository.RoleRepository;
import com.eprocure.iam.domain.repository.UserRepository;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PermissionResolutionServiceTest {
    @Test
    void should_cache_role_permissions_and_refresh_when_role_permission_changes() {
        RoleRepository roleRepository = mock(RoleRepository.class);
        FakePermissionCachePort permissionCachePort = new FakePermissionCachePort();
        PermissionResolutionService service = new PermissionResolutionService(
                mock(UserRepository.class),
                roleRepository,
                permissionCachePort,
                15);
        when(roleRepository.findPermissionCodesByRoleCode("REQUESTER"))
                .thenReturn(Set.of("IAM_PROFILE_READ"));

        assertThat(service.resolveByRoleCodes(Set.of("requester"))).containsExactly("IAM_PROFILE_READ");
        assertThat(permissionCachePort.permissionsByRole.get("REQUESTER")).containsExactly("IAM_PROFILE_READ");

        service.refreshRolePermissions("requester", Set.of("ADMIN_USER_VIEW"));

        assertThat(service.resolveByRoleCodes(Set.of("REQUESTER"))).containsExactly("ADMIN_USER_VIEW");
        verify(roleRepository).findPermissionCodesByRoleCode("REQUESTER");
    }

    private static final class FakePermissionCachePort implements PermissionCachePort {
        private final Map<String, Set<String>> permissionsByRole = new HashMap<>();

        @Override
        public Optional<Set<String>> findRolePermissions(String roleCode) {
            return Optional.ofNullable(permissionsByRole.get(roleCode));
        }

        @Override
        public void storeRolePermissions(String roleCode, Set<String> permissionCodes, Duration ttl) {
            permissionsByRole.put(roleCode, Set.copyOf(permissionCodes));
        }

        @Override
        public void evictRolePermissions(String roleCode) {
            permissionsByRole.remove(roleCode);
        }
    }
}
