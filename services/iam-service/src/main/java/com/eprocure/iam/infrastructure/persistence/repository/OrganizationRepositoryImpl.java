package com.eprocure.iam.infrastructure.persistence.repository;

import com.eprocure.iam.domain.model.Department;
import com.eprocure.iam.domain.model.User;
import com.eprocure.iam.domain.repository.OrganizationRepository;
import com.eprocure.iam.domain.repository.Page;
import com.eprocure.iam.infrastructure.persistence.entity.DepartmentDbEntity;
import com.eprocure.iam.infrastructure.persistence.entity.UserDbEntity;
import com.eprocure.iam.infrastructure.persistence.entity.UserPageDbEntity;
import com.eprocure.iam.infrastructure.persistence.mapper.OrganizationMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class OrganizationRepositoryImpl implements OrganizationRepository {
    private final OrganizationMapper organizationMapper;
    private final ObjectMapper objectMapper;

    public OrganizationRepositoryImpl(
            OrganizationMapper organizationMapper,
            @Qualifier("domainObjectMapper") ObjectMapper objectMapper) {
        this.organizationMapper = organizationMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Department> findAllDepartments() {
        return organizationMapper.findAllDepartments().stream().map(this::toDepartment).toList();
    }

    @Override
    public Optional<Department> findDepartmentById(UUID departmentId) {
        return Optional.ofNullable(organizationMapper.findDepartmentById(departmentId)).map(this::toDepartment);
    }

    @Override
    public long countActiveMembersByDepartmentId(UUID departmentId) {
        return organizationMapper.countActiveMembersByDepartmentId(departmentId);
    }

    @Override
    public Page<User> findDepartmentMembers(UUID departmentId, int offset, int limit) {
        List<UserPageDbEntity> rows = organizationMapper.findDepartmentMembers(departmentId, offset, limit);
        long totalElements = rows.isEmpty() ? 0 : rows.get(0).totalElements;
        return new Page<>(rows.stream().map(this::toUser).toList(), totalElements);
    }

    @Override
    public List<User> findApprovers(String roleCode, UUID departmentId, UUID excludedUserId, int limit) {
        return organizationMapper.findApprovers(roleCode, departmentId, excludedUserId, limit).stream()
                .map(this::toUser)
                .toList();
    }

    private Department toDepartment(DepartmentDbEntity entity) {
        return objectMapper.convertValue(entity, Department.class);
    }

    private User toUser(UserDbEntity entity) {
        return objectMapper.convertValue(entity, User.class);
    }
}
