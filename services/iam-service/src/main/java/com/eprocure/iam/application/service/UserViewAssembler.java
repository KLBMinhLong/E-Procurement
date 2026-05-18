package com.eprocure.iam.application.service;

import com.eprocure.iam.domain.model.Department;
import com.eprocure.iam.domain.model.User;
import com.eprocure.iam.domain.repository.DepartmentRepository;
import com.eprocure.iam.domain.repository.UserRepository;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class UserViewAssembler {
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final PermissionResolutionService permissionResolutionService;

    public UserViewAssembler(
            DepartmentRepository departmentRepository,
            UserRepository userRepository,
            PermissionResolutionService permissionResolutionService) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.permissionResolutionService = permissionResolutionService;
    }

    public UserSummaryView toSummary(User user) {
        return new UserSummaryView(
                user.getId(),
                user.getEmployeeCode(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getAvatarUrl().orElse(null),
                departmentRepository.findById(user.getDepartmentId()).map(this::toDepartmentView).orElse(null),
                user.getStatus());
    }

    public UserDetailView toDetail(User user) {
        Set<String> roles = userRepository.findRoleCodesByUserId(user.getId());
        return new UserDetailView(
                user.getId(),
                user.getEmployeeCode(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getAvatarUrl().orElse(null),
                departmentRepository.findById(user.getDepartmentId()).map(this::toDepartmentView).orElse(null),
                user.getStatus(),
                user.getPhone().orElse(null),
                user.getOrgNodeId().orElse(null),
                roles,
                permissionResolutionService.resolveByRoleCodes(roles),
                user.isTwoFactorEnabled(),
                user.getLastLoginAt().orElse(null),
                user.getCreatedAt());
    }

    private DepartmentView toDepartmentView(Department department) {
        return new DepartmentView(department.getId(), department.getCode(), department.getName());
    }
}
