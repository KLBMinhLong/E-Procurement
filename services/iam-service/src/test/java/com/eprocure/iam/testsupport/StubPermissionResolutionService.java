package com.eprocure.iam.testsupport;

import static org.mockito.Mockito.mock;

import com.eprocure.iam.application.port.out.PermissionCachePort;
import com.eprocure.iam.application.service.PermissionResolutionService;
import com.eprocure.iam.domain.repository.RoleRepository;
import com.eprocure.iam.domain.repository.UserRepository;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;

public class StubPermissionResolutionService extends PermissionResolutionService {
    private Set<String> permissions = Set.of();
    private String refreshedRoleCode;
    private Set<String> refreshedPermissionCodes = Set.of();

    public StubPermissionResolutionService() {
        super(mock(UserRepository.class), mock(RoleRepository.class), new NoopPermissionCachePort(), 15);
    }

    @Override
    public Set<String> resolveByRoleCodes(Set<String> roleCodes) {
        return permissions;
    }

    @Override
    public void refreshRolePermissions(String roleCode, Set<String> permissionCodes) {
        this.refreshedRoleCode = roleCode;
        this.refreshedPermissionCodes = Set.copyOf(permissionCodes);
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = Set.copyOf(permissions);
    }

    public String refreshedRoleCode() {
        return refreshedRoleCode;
    }

    public Set<String> refreshedPermissionCodes() {
        return refreshedPermissionCodes;
    }

    private static final class NoopPermissionCachePort implements PermissionCachePort {
        @Override
        public Optional<Set<String>> findRolePermissions(String roleCode) {
            return Optional.empty();
        }

        @Override
        public void storeRolePermissions(String roleCode, Set<String> permissionCodes, Duration ttl) {
        }

        @Override
        public void evictRolePermissions(String roleCode) {
        }
    }
}
