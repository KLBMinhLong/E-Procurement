package com.eprocure.iam.domain.repository;

import com.eprocure.iam.domain.model.Department;
import java.util.Optional;
import java.util.UUID;

public interface DepartmentRepository {
    Optional<Department> findById(UUID id);
}
