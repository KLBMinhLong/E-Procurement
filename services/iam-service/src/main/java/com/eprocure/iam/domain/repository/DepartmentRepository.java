package com.eprocure.iam.domain.repository;

import com.eprocure.iam.domain.model.Department;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface DepartmentRepository {
    Optional<Department> findById(UUID id);

    Optional<Department> findByIdIncludingInactive(UUID id);

    Optional<Department> findByCode(String code);

    boolean existsActiveByCode(String code);

    boolean existsActiveByCodeExceptId(String code, UUID excludedId);

    boolean isDescendant(UUID candidateParentId, UUID departmentId);

    long countActiveMembersByDepartmentId(UUID departmentId);

    long countActiveChildrenByDepartmentId(UUID departmentId);

    void save(Department department, UUID actorId);

    void update(Department department, UUID actorId);

    void deactivate(UUID departmentId, UUID actorId, Instant deletedAt);
}
