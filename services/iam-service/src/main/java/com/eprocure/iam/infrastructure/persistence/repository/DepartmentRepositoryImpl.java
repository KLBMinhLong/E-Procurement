package com.eprocure.iam.infrastructure.persistence.repository;

import com.eprocure.iam.domain.model.Department;
import com.eprocure.iam.domain.repository.DepartmentRepository;
import com.eprocure.iam.infrastructure.persistence.entity.DepartmentDbEntity;
import com.eprocure.iam.infrastructure.persistence.mapper.DepartmentMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class DepartmentRepositoryImpl implements DepartmentRepository {
    private final DepartmentMapper departmentMapper;
    private final ObjectMapper objectMapper;

    public DepartmentRepositoryImpl(
            DepartmentMapper departmentMapper,
            @Qualifier("domainObjectMapper") ObjectMapper objectMapper) {
        this.departmentMapper = departmentMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<Department> findById(UUID id) {
        return Optional.ofNullable(departmentMapper.findById(id)).map(this::toDomain);
    }

    @Override
    public Optional<Department> findByIdIncludingInactive(UUID id) {
        return Optional.ofNullable(departmentMapper.findByIdIncludingInactive(id)).map(this::toDomain);
    }

    @Override
    public Optional<Department> findByCode(String code) {
        return Optional.ofNullable(departmentMapper.findByCode(code)).map(this::toDomain);
    }

    @Override
    public boolean existsActiveByCode(String code) {
        return departmentMapper.existsActiveByCode(code);
    }

    @Override
    public boolean existsActiveByCodeExceptId(String code, UUID excludedId) {
        return departmentMapper.existsActiveByCodeExceptId(code, excludedId);
    }

    @Override
    public boolean isDescendant(UUID candidateParentId, UUID departmentId) {
        return departmentMapper.isDescendant(candidateParentId, departmentId);
    }

    @Override
    public long countActiveMembersByDepartmentId(UUID departmentId) {
        return departmentMapper.countActiveMembersByDepartmentId(departmentId);
    }

    @Override
    public long countActiveChildrenByDepartmentId(UUID departmentId) {
        return departmentMapper.countActiveChildrenByDepartmentId(departmentId);
    }

    @Override
    public void save(Department department, UUID actorId) {
        departmentMapper.insert(toEntity(department), actorId);
    }

    @Override
    public void update(Department department, UUID actorId) {
        departmentMapper.update(toEntity(department), actorId);
    }

    @Override
    public void deactivate(UUID departmentId, UUID actorId, Instant deletedAt) {
        departmentMapper.deactivate(departmentId, actorId, deletedAt);
    }

    private Department toDomain(DepartmentDbEntity entity) {
        return objectMapper.convertValue(entity, Department.class);
    }

    private DepartmentDbEntity toEntity(Department department) {
        return objectMapper.convertValue(department, DepartmentDbEntity.class);
    }
}
