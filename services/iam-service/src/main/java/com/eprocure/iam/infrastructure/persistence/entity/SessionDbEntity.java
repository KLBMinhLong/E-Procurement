package com.eprocure.iam.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

public class SessionDbEntity {
    public UUID id;
    public UUID userId;
    public String tokenHash;
    public String ipAddress;
    public String userAgent;
    public Instant issuedAt;
    public Instant expiresAt;
    public boolean revoked;
    public Instant revokedAt;
    public UUID revokedBy;
}
