package com.eprocure.iam.application.service;

import com.eprocure.iam.domain.model.UserStatus;
import java.util.UUID;

public record UserSummaryView(
        UUID id,
        String employeeCode,
        String username,
        String fullName,
        String email,
        String avatarUrl,
        DepartmentView department,
        UserStatus status) {
}
