package com.eprocure.admin.domain.model;

import java.util.Objects;

public record InfrastructureHealth(
        InfrastructureComponentHealth postgresql,
        InfrastructureComponentHealth redis,
        InfrastructureComponentHealth kafka) {

    public InfrastructureHealth {
        postgresql = Objects.requireNonNull(postgresql, "postgresql must not be null");
        redis = Objects.requireNonNull(redis, "redis must not be null");
        kafka = Objects.requireNonNull(kafka, "kafka must not be null");
    }
}
