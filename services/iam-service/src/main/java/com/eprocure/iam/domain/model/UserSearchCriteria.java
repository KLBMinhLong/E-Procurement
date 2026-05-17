package com.eprocure.iam.domain.model;

import java.util.UUID;

public record UserSearchCriteria(
        UserStatus status,
        UUID departmentId,
        String roleCode,
        String query) {

    public UserSearchCriteria {
        roleCode = normalize(roleCode);
        query = normalize(query);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
