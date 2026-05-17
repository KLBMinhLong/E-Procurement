package com.eprocure.iam.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class SessionRecord {
    private UUID id;
    private UUID userId;
    private String tokenHash;
    private String ipAddress;
    private String userAgent;
    private Instant issuedAt;
    private Instant expiresAt;
    private boolean revoked;
    private Instant revokedAt;
    private UUID revokedBy;

    private SessionRecord() {
    }

    private SessionRecord(
            UUID id,
            UUID userId,
            String tokenHash,
            String ipAddress,
            String userAgent,
            Instant issuedAt,
            Instant expiresAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.tokenHash = requireTokenHash(tokenHash);
        this.ipAddress = normalize(ipAddress).orElse(null);
        this.userAgent = normalize(userAgent).orElse(null);
        this.issuedAt = Objects.requireNonNull(issuedAt, "issuedAt must not be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        this.revoked = false;
    }

    public static SessionRecord issue(
            UUID id,
            UUID userId,
            String tokenHash,
            String ipAddress,
            String userAgent,
            Instant issuedAt,
            Instant expiresAt) {
        return new SessionRecord(id, userId, tokenHash, ipAddress, userAgent, issuedAt, expiresAt);
    }

    public boolean isActive(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        return !revoked && expiresAt.isAfter(now);
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Optional<String> getIpAddress() {
        return Optional.ofNullable(ipAddress);
    }

    public Optional<String> getUserAgent() {
        return Optional.ofNullable(userAgent);
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public Optional<Instant> getRevokedAt() {
        return Optional.ofNullable(revokedAt);
    }

    public Optional<UUID> getRevokedBy() {
        return Optional.ofNullable(revokedBy);
    }

    private static String requireTokenHash(String tokenHash) {
        if (tokenHash == null || !tokenHash.matches("^[a-f0-9]{64}$")) {
            throw new IllegalArgumentException("tokenHash must be a 64-char lowercase SHA-256 hex");
        }
        return tokenHash;
    }

    private static Optional<String> normalize(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value.trim());
    }
}
