package com.eprocure.iam.application.service;

import com.eprocure.iam.domain.model.Department;
import com.eprocure.iam.domain.model.User;
import com.eprocure.iam.domain.repository.DepartmentRepository;
import com.eprocure.iam.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserViewAssembler {
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    public UserViewAssembler(DepartmentRepository departmentRepository, UserRepository userRepository) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
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
                userRepository.findRoleCodesByUserId(user.getId()),
                userRepository.findPermissionCodesByUserId(user.getId()),
                user.isTwoFactorEnabled(),
                user.getLastLoginAt().orElse(null),
                user.getCreatedAt());
    }

    private DepartmentView toDepartmentView(Department department) {
        return new DepartmentView(department.getId(), department.getCode(), department.getName());
    }
}
