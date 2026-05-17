package com.eprocure.iam.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class Permission {
    private UUID id;
    private String code;
    private String name;
    private String description;
    private String service;
    private Instant createdAt;

    private Permission() {
    }

    private Permission(UUID id, String code, String name, String description, String service, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.code = requireText(code, "code").toUpperCase();
        this.name = requireText(name, "name");
        this.description = normalize(description);
        this.service = requireText(service, "service").toUpperCase();
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static Permission create(UUID id, String code, String name, String description, String service, Instant createdAt) {
        return new Permission(id, code, name, description, service, createdAt);
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

    public String getService() {
        return service;
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
