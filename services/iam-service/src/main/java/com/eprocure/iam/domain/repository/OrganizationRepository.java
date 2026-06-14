package com.eprocure.iam.domain.repository;

import com.eprocure.iam.domain.model.Department;
import com.eprocure.iam.domain.model.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository {
    List<Department> findAllDepartments();

    Optional<Department> findDepartmentById(UUID departmentId);

    long countActiveMembersByDepartmentId(UUID departmentId);

    Page<User> findDepartmentMembers(UUID departmentId, int offset, int limit);

    List<User> findApprovers(String roleCode, UUID departmentId, UUID excludedUserId, int limit);

    List<User> findApproversByPermission(String permissionCode, UUID departmentId, UUID excludedUserId, int limit);
}
