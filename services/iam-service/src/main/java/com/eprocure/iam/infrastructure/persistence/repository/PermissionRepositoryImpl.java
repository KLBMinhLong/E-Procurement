package com.eprocure.iam.infrastructure.persistence.repository;

import com.eprocure.iam.domain.model.Permission;
import com.eprocure.iam.domain.repository.PermissionRepository;
import com.eprocure.iam.infrastructure.persistence.entity.PermissionDbEntity;
import com.eprocure.iam.infrastructure.persistence.mapper.PermissionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class PermissionRepositoryImpl implements PermissionRepository {
    private final PermissionMapper permissionMapper;
    private final ObjectMapper objectMapper;

    public PermissionRepositoryImpl(PermissionMapper permissionMapper, @Qualifier("domainObjectMapper") ObjectMapper objectMapper) {
        this.permissionMapper = permissionMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Permission> findAll() {
        return permissionMapper.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Set<String> findExistingCodes(Set<String> codes) {
        return Set.copyOf(permissionMapper.findExistingCodes(codes));
    }

    private Permission toDomain(PermissionDbEntity entity) {
        return objectMapper.convertValue(entity, Permission.class);
    }
}
