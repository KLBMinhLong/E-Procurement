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
    private Instant updatedAt;
    private boolean deleted;

    private Department() {
    }

    private Department(
            UUID id,
            String code,
            String name,
            UUID parentId,
            UUID headUserId,
            Instant createdAt,
            Instant updatedAt,
            boolean deleted) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.code = requireText(code, "code");
        this.name = requireText(name, "name");
        this.parentId = parentId;
        this.headUserId = headUserId;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = updatedAt;
        this.deleted = deleted;
    }

    public static Department create(UUID id, String code, String name, Instant createdAt) {
        return create(id, code, name, null, null, createdAt);
    }

    public static Department create(
            UUID id,
            String code,
            String name,
            UUID parentId,
            UUID headUserId,
            Instant createdAt) {
        return new Department(id, code, name, parentId, headUserId, createdAt, createdAt, false);
    }

    public static Department reconstitute(
            UUID id,
            String code,
            String name,
            UUID parentId,
            UUID headUserId,
            Instant createdAt,
            Instant updatedAt,
            boolean deleted) {
        return new Department(id, code, name, parentId, headUserId, createdAt, updatedAt, deleted);
    }

    public void update(String code, String name, UUID parentId, UUID headUserId, Instant updatedAt) {
        this.code = requireText(code, "code");
        this.name = requireText(name, "name");
        this.parentId = parentId;
        this.headUserId = headUserId;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public void deactivate(Instant updatedAt) {
        this.deleted = true;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
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

    public Optional<Instant> getUpdatedAt() {
        return Optional.ofNullable(updatedAt);
    }

    public boolean isDeleted() {
        return deleted;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
