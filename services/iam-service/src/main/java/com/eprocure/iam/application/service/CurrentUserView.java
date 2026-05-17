package com.eprocure.iam.application.service;

import com.eprocure.iam.domain.model.UserStatus;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record CurrentUserView(
        UUID id,
        String employeeCode,
        String username,
        String fullName,
        String email,
        String phone,
        String avatarUrl,
        DepartmentView department,
        UUID orgNodeId,
        Set<String> roles,
        Set<String> permissions,
        UserStatus status,
        boolean twoFactorEnabled,
        Instant lastLoginAt,
        Instant createdAt) {
}
