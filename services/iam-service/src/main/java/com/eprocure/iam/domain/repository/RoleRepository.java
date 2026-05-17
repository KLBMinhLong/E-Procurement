package com.eprocure.iam.domain.repository;

import com.eprocure.iam.domain.model.Role;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface RoleRepository {
    List<Role> findAll();

    Optional<Role> findByCode(String code);

    Set<String> findExistingCodes(Set<String> codes);

    Set<String> findPermissionCodesByRoleCode(String code);

    void save(Role role, Set<String> permissionCodes, UUID actorId);

    void replacePermissions(String roleCode, Set<String> permissionCodes, UUID actorId);
}
