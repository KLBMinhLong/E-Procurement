package com.eprocure.iam.application.service;

import java.util.Set;

public record RoleDetailView(
        String code,
        String name,
        String description,
        Set<String> permissions,
        boolean isSystemRole) {
    public RoleDetailView {
        permissions = Set.copyOf(permissions);
    }
}
