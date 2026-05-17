package com.eprocure.iam.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

public class PasswordResetTokenDbEntity {
    public UUID id;
    public UUID userId;
    public String tokenHash;
    public Instant expiresAt;
    public Instant usedAt;
    public Instant createdAt;
}
