package com.eprocure.iam.application.service;

import com.eprocure.iam.application.port.out.PermissionCachePort;
import com.eprocure.iam.domain.repository.RoleRepository;
import com.eprocure.iam.domain.repository.UserRepository;
import java.time.Duration;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PermissionResolutionService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionCachePort permissionCachePort;
    private final Duration permissionCacheTtl;

    public PermissionResolutionService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PermissionCachePort permissionCachePort,
            @Value("${eprocure.session.permission-cache-ttl-minutes:15}") long permissionCacheTtlMinutes) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionCachePort = permissionCachePort;
        this.permissionCacheTtl = Duration.ofMinutes(permissionCacheTtlMinutes);
    }

    public Set<String> resolveByUserId(UUID userId) {
        return resolveByRoleCodes(userRepository.findRoleCodesByUserId(userId));
    }

    public Set<String> resolveByRoleCodes(Set<String> roleCodes) {
        return normalize(roleCodes).stream()
                .flatMap(roleCode -> findRolePermissions(roleCode).stream())
                .collect(Collectors.toUnmodifiableSet());
    }

    public void refreshRolePermissions(String roleCode, Set<String> permissionCodes) {
        permissionCachePort.storeRolePermissions(normalize(roleCode), normalize(permissionCodes), permissionCacheTtl);
    }

    public void evictRolePermissions(String roleCode) {
        permissionCachePort.evictRolePermissions(normalize(roleCode));
    }

    private Set<String> findRolePermissions(String roleCode) {
        return permissionCachePort.findRolePermissions(roleCode)
                .orElseGet(() -> {
                    Set<String> permissionCodes = roleRepository.findPermissionCodesByRoleCode(roleCode);
                    permissionCachePort.storeRolePermissions(roleCode, permissionCodes, permissionCacheTtl);
                    return permissionCodes;
                });
    }

    private String normalize(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }

    private Set<String> normalize(Collection<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return Set.of();
        }
        return codes.stream()
                .filter(code -> code != null && !code.isBlank())
                .map(this::normalize)
                .collect(Collectors.toUnmodifiableSet());
    }
}
