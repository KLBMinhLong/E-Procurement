package com.eprocure.admin.presentation.response;

import java.util.UUID;

public record DepartmentResponse(
        UUID id,
        String code,
        String name,
        UUID parentId,
        UUID headUserId,
        String glAccountPrefix,
        long memberCount,
        long childCount,
        boolean isDeleted) {
}
