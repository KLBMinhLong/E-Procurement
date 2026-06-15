package com.eprocure.admin.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record ServiceHealth(
        String name,
        ServiceStatus status,
        long responseTime,
        Optional<String> uptime,
        Instant lastCheck) {

    public ServiceHealth {
        name = Objects.requireNonNull(name, "name must not be null").trim();
        status = Objects.requireNonNullElse(status, ServiceStatus.UNKNOWN);
        responseTime = Math.max(responseTime, 0L);
        uptime = uptime == null ? Optional.empty() : uptime;
        lastCheck = Objects.requireNonNull(lastCheck, "lastCheck must not be null");
    }
}
