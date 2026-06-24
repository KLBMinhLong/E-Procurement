package com.eprocure.admin.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record ServiceConfig(
        String serviceName,
        String displayName,
        String baseUrl,
        ServiceStatus status,
        Optional<String> version,
        List<EnvVariable> variables,
        Optional<Instant> lastHealthCheck) {

    public ServiceConfig {
        serviceName = Objects.requireNonNull(serviceName, "serviceName must not be null").trim();
        displayName = Objects.requireNonNull(displayName, "displayName must not be null").trim();
        baseUrl = Objects.requireNonNull(baseUrl, "baseUrl must not be null").trim();
        status = Objects.requireNonNullElse(status, ServiceStatus.UNKNOWN);
        version = version == null ? Optional.empty() : version;
        variables = List.copyOf(variables == null ? List.of() : variables);
        lastHealthCheck = lastHealthCheck == null ? Optional.empty() : lastHealthCheck;
    }

    public ServiceConfig masked() {
        return new ServiceConfig(
                serviceName,
                displayName,
                baseUrl,
                status,
                version,
                variables.stream().map(EnvVariable::masked).toList(),
                lastHealthCheck);
    }

    public ServiceConfig withHealth(ServiceStatus healthStatus, Instant checkedAt) {
        return new ServiceConfig(
                serviceName,
                displayName,
                baseUrl,
                healthStatus,
                version,
                variables,
                Optional.of(Objects.requireNonNull(checkedAt, "checkedAt must not be null")));
    }
}
