package com.eprocure.iam.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

public class ActiveSessionDbEntity {
    public UUID sessionId;
    public UUID userId;
    public String username;
    public String fullName;
    public String ipAddress;
    public String userAgent;
    public Instant issuedAt;
    public Instant lastActivityAt;
    public Instant expiresAt;
    public long totalElements;
}
