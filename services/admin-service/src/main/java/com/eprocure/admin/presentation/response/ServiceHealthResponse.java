package com.eprocure.admin.presentation.response;

import java.time.Instant;

public record ServiceHealthResponse(
        String name,
        String status,
        long responseTime,
        String uptime,
        Instant lastCheck) {
}
