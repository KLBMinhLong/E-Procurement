package com.eprocure.admin.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record SystemHealth(
        ServiceStatus overallStatus,
        List<ServiceHealth> services,
        InfrastructureHealth infrastructure,
        Instant checkedAt) {

    public SystemHealth {
        overallStatus = Objects.requireNonNullElse(overallStatus, ServiceStatus.UNKNOWN);
        services = List.copyOf(services == null ? List.of() : services);
        infrastructure = Objects.requireNonNull(infrastructure, "infrastructure must not be null");
        checkedAt = Objects.requireNonNull(checkedAt, "checkedAt must not be null");
    }
}
