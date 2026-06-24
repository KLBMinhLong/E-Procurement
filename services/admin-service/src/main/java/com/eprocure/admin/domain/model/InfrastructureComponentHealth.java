package com.eprocure.admin.domain.model;

import java.util.Map;
import java.util.Objects;

public record InfrastructureComponentHealth(
        String name,
        ServiceStatus status,
        Map<String, Object> metrics) {

    public InfrastructureComponentHealth {
        name = Objects.requireNonNull(name, "name must not be null").trim();
        status = Objects.requireNonNullElse(status, ServiceStatus.UNKNOWN);
        metrics = Map.copyOf(metrics == null ? Map.of() : metrics);
    }
}
