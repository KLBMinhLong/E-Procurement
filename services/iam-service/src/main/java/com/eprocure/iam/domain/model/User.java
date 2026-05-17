package com.eprocure.iam.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class User {
    private UUID id;
    private String employeeCode;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private String avatarUrl;
    private UUID departmentId;
    private UUID orgNodeId;
    private String keycloakUsername;
    private UserStatus status;
    private boolean twoFactorEnabled;
    private Instant lastLoginAt;
    private Instant createdAt;

    private User() {
    }

    private User(
            UUID id,
            String employeeCode,
            String username,
            String email,
            String fullName,
            String phone,
            UUID departmentId,
            UUID orgNodeId,
            UserStatus status,
            boolean twoFactorEnabled,
            Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.employeeCode = requireText(employeeCode, "employeeCode");
        this.username = requireText(username, "username");
        this.email = requireText(email, "email").toLowerCase();
        this.fullName = requireText(fullName, "fullName");
        this.phone = normalize(phone);
        this.departmentId = Objects.requireNonNull(departmentId, "departmentId must not be null");
        this.orgNodeId = orgNodeId;
        this.keycloakUsername = this.username;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.twoFactorEnabled = twoFactorEnabled;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static User create(
            UUID id,
            String employeeCode,
            String username,
            String email,
            String fullName,
            UUID departmentId,
            UserStatus status,
            Instant createdAt) {
        return new User(id, employeeCode, username, email, fullName, null, departmentId, null, status, false, createdAt);
    }

    public static User create(
            UUID id,
            String employeeCode,
            String username,
            String email,
            String fullName,
            String phone,
            UUID departmentId,
            UUID orgNodeId,
            UserStatus status,
            boolean twoFactorEnabled,
            Instant createdAt) {
        return new User(id, employeeCode, username, email, fullName, phone, departmentId, orgNodeId, status, twoFactorEnabled, createdAt);
    }

    public boolean canLogin() {
        return status == UserStatus.ACTIVE;
    }

    public boolean isLocked() {
        return status == UserStatus.LOCKED;
    }

    public void updateProfile(String fullName, String phone, UUID departmentId, UUID orgNodeId) {
        this.fullName = requireText(fullName, "fullName");
        this.phone = normalize(phone);
        this.departmentId = Objects.requireNonNull(departmentId, "departmentId must not be null");
        this.orgNodeId = orgNodeId;
    }

    public void changeStatus(UserStatus status) {
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public UUID getId() {
        return id;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public Optional<String> getPhone() {
        return Optional.ofNullable(phone);
    }

    public Optional<String> getAvatarUrl() {
        return Optional.ofNullable(avatarUrl);
    }

    public UUID getDepartmentId() {
        return departmentId;
    }

    public Optional<UUID> getOrgNodeId() {
        return Optional.ofNullable(orgNodeId);
    }

    public String getKeycloakUsername() {
        return keycloakUsername == null || keycloakUsername.isBlank() ? username : keycloakUsername;
    }

    public UserStatus getStatus() {
        return status;
    }

    public boolean isTwoFactorEnabled() {
        return twoFactorEnabled;
    }

    public Optional<Instant> getLastLoginAt() {
        return Optional.ofNullable(lastLoginAt);
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
