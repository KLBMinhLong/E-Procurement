package com.eprocure.iam.infrastructure.persistence.repository;

import com.eprocure.iam.domain.model.Department;
import com.eprocure.iam.domain.repository.DepartmentRepository;
import com.eprocure.iam.infrastructure.persistence.entity.DepartmentDbEntity;
import com.eprocure.iam.infrastructure.persistence.mapper.DepartmentMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private Department toDomain(DepartmentDbEntity entity) {
        return objectMapper.convertValue(entity, Department.class);
    }
}
