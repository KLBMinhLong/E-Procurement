package com.eprocure.iam.domain.model;

import java.util.Locale;

public enum UserSort {
    CREATED_AT,
    FULL_NAME,
    USERNAME,
    STATUS;

    public static UserSort from(String value) {
        if (value == null || value.isBlank()) {
            return CREATED_AT;
        }
        String normalized = value.trim()
                .replace("-", "_")
                .replace(".", "_")
                .toUpperCase(Locale.ROOT);
        if ("CREATEDAT".equals(normalized) || "CREATED_AT".equals(normalized)) {
            return CREATED_AT;
        }
        if ("FULLNAME".equals(normalized) || "FULL_NAME".equals(normalized)) {
            return FULL_NAME;
        }
        return switch (normalized) {
            case "USERNAME" -> USERNAME;
            case "STATUS" -> STATUS;
            default -> CREATED_AT;
        };
    }
}
