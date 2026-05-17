package com.eprocure.iam.infrastructure.persistence.entity;

import com.eprocure.iam.domain.model.UserStatus;
import java.time.Instant;
import java.util.UUID;

public class UserDbEntity {
    public UUID id;
    public String employeeCode;
    public String username;
    public String email;
    public String fullName;
    public String phone;
    public String avatarUrl;
    public UUID departmentId;
    public UUID orgNodeId;
    public String keycloakUsername;
    public UserStatus status;
    public boolean twoFactorEnabled;
    public String twoFactorSecretEncrypted;
    public String twoFactorPendingSecretEncrypted;
    public Instant twoFactorConfirmedAt;
    public Instant lastLoginAt;
    public Instant createdAt;
}
