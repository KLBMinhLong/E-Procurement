package com.eprocure.iam.application.service;

import com.eprocure.iam.domain.model.UserStatus;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserDetailView(
        UUID id,
        String employeeCode,
        String username,
        String fullName,
        String email,
        String avatarUrl,
        DepartmentView department,
        UserStatus status,
        String phone,
        UUID orgNodeId,
        Set<String> roles,
        Set<String> permissions,
        boolean twoFactorEnabled,
        Instant lastLoginAt,
        Instant createdAt) {
    public UserDetailView {
        roles = Set.copyOf(roles);
        permissions = Set.copyOf(permissions);
    }
}
