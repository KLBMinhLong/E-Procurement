package com.eprocure.iam.application.service;

import com.eprocure.iam.domain.model.UserStatus;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserSummaryView(
        UUID id,
        String employeeCode,
        String username,
        String fullName,
        String email,
        String avatarUrl,
        DepartmentView department,
        UserStatus status,
        String phone,
        Set<String> roles,
        Instant createdAt) {
    public UserSummaryView {
        roles = roles != null ? Set.copyOf(roles) : Set.of();
    }
}
