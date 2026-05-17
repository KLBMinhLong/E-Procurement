package com.eprocure.iam.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class Role {
    private UUID id;
    private String code;
    private String name;
    private String description;
    private boolean systemRole;
    private Instant createdAt;

    private Role() {
    }

    private Role(UUID id, String code, String name, String description, boolean systemRole, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.code = requireText(code, "code").toUpperCase();
        this.name = requireText(name, "name");
        this.description = normalize(description);
        this.systemRole = systemRole;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static Role create(UUID id, String code, String name, String description, boolean systemRole, Instant createdAt) {
        return new Role(id, code, name, description, systemRole, createdAt);
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

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public boolean isSystemRole() {
        return systemRole;
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

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
