package com.eprocure.iam.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class Department {
    private UUID id;
    private String code;
    private String name;
    private UUID parentId;
    private UUID headUserId;
    private Instant createdAt;

    private Department() {
    }

    private Department(UUID id, String code, String name, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.code = requireText(code, "code");
        this.name = requireText(name, "name");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static Department create(UUID id, String code, String name, Instant createdAt) {
        return new Department(id, code, name, createdAt);
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Optional<UUID> getParentId() {
        return Optional.ofNullable(parentId);
    }

    public Optional<UUID> getHeadUserId() {
        return Optional.ofNullable(headUserId);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
