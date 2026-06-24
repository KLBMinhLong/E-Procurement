package com.eprocure.admin.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record EnvVariable(
        String key,
        String value,
        boolean sensitive,
        Optional<String> description,
        Optional<Instant> lastUpdatedAt,
        Optional<String> lastUpdatedBy) {

    public EnvVariable {
        key = Objects.requireNonNull(key, "key must not be null").trim();
        value = Objects.requireNonNullElse(value, "");
        description = description == null ? Optional.empty() : description;
        lastUpdatedAt = lastUpdatedAt == null ? Optional.empty() : lastUpdatedAt;
        lastUpdatedBy = lastUpdatedBy == null ? Optional.empty() : lastUpdatedBy;
    }

    public EnvVariable masked() {
        return new EnvVariable(
                key,
                sensitive ? "***" : value,
                sensitive,
                description,
                lastUpdatedAt,
                lastUpdatedBy);
    }
}
