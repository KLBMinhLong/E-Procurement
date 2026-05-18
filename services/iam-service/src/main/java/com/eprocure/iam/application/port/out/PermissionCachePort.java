package com.eprocure.iam.application.port.out;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

public interface PermissionCachePort {
    Optional<Set<String>> findRolePermissions(String roleCode);

    void storeRolePermissions(String roleCode, Set<String> permissionCodes, Duration ttl);

    void evictRolePermissions(String roleCode);
}
