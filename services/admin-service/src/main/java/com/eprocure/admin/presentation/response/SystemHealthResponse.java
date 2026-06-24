package com.eprocure.admin.presentation.response;

import java.time.Instant;
import java.util.List;

public record SystemHealthResponse(
        String overallStatus,
        List<ServiceHealthResponse> services,
        InfrastructureHealthResponse infrastructure,
        Instant checkedAt) {
}
