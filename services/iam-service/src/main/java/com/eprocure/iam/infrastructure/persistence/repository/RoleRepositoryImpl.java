package com.eprocure.iam.infrastructure.persistence.repository;

import com.eprocure.iam.domain.model.Role;
import com.eprocure.iam.domain.repository.RoleRepository;
import com.eprocure.iam.infrastructure.persistence.entity.RoleDbEntity;
import com.eprocure.iam.infrastructure.persistence.mapper.RoleMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class RoleRepositoryImpl implements RoleRepository {
    private final RoleMapper roleMapper;
    private final ObjectMapper objectMapper;

    public RoleRepositoryImpl(RoleMapper roleMapper, @Qualifier("domainObjectMapper") ObjectMapper objectMapper) {
        this.roleMapper = roleMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Role> findAll() {
        return roleMapper.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Role> findByCode(String code) {
        return Optional.ofNullable(roleMapper.findByCode(code)).map(this::toDomain);
    }

    @Override
    public Set<String> findExistingCodes(Set<String> codes) {
        return Set.copyOf(roleMapper.findExistingCodes(codes));
    }

    @Override
    public Set<String> findPermissionCodesByRoleCode(String code) {
        return Set.copyOf(roleMapper.findPermissionCodesByRoleCode(code));
    }

    @Override
    public void save(Role role, Set<String> permissionCodes, UUID actorId) {
        roleMapper.insert(toEntity(role), actorId);
        replacePermissions(role.getCode(), permissionCodes, actorId);
    }

    @Override
    public void update(Role role, UUID actorId) {
        roleMapper.update(toEntity(role), actorId);
    }

    @Override
    public void replacePermissions(String roleCode, Set<String> permissionCodes, UUID actorId) {
        roleMapper.softDeleteRolePermissionsNotIn(roleCode, permissionCodes, actorId);
        permissionCodes.forEach(permissionCode -> roleMapper.upsertRolePermission(roleCode, permissionCode, actorId));
    }

    private Role toDomain(RoleDbEntity entity) {
        return objectMapper.convertValue(entity, Role.class);
    }

    private RoleDbEntity toEntity(Role role) {
        return objectMapper.convertValue(role, RoleDbEntity.class);
    }
}
