package com.eprocure.admin.application.service;

import java.util.UUID;

public record DepartmentAdminView(
        UUID id,
        String code,
        String name,
        UUID parentId,
        UUID headUserId,
        long memberCount,
        long childCount,
        boolean deleted) {
}
