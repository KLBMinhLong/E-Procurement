package com.eprocure.admin.presentation.response;

import java.time.Instant;
import java.util.List;

public record ServiceConfigResponse(
        String serviceName,
        String displayName,
        String status,
        String version,
        List<EnvVariableResponse> variables,
        Instant lastHealthCheck) {
}
