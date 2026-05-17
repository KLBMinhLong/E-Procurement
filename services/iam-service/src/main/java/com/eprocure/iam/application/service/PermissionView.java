package com.eprocure.iam.application.service;

public record PermissionView(
        String code,
        String name,
        String description,
        String service) {
}
