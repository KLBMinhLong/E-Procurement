package com.eprocure.admin.presentation.response;

import java.time.Instant;

public record EnvVariableResponse(
        String key,
        String value,
        boolean isSensitive,
        String description,
        Instant lastUpdatedAt,
        String lastUpdatedBy) {
}
