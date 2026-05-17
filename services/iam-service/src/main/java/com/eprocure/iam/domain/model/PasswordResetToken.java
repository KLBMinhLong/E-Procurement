package com.eprocure.iam.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class PasswordResetToken {
    private UUID id;
    private UUID userId;
    private String tokenHash;
    private Instant expiresAt;
    private Instant usedAt;
    private Instant createdAt;

    private PasswordResetToken() {
    }

    private PasswordResetToken(UUID id, UUID userId, String tokenHash, Instant expiresAt, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.tokenHash = requireTokenHash(tokenHash);
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static PasswordResetToken issue(
            UUID id,
            UUID userId,
            String tokenHash,
            Instant expiresAt,
            Instant createdAt) {
        return new PasswordResetToken(id, userId, tokenHash, expiresAt, createdAt);
    }

    public boolean isActive(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        return usedAt == null && expiresAt.isAfter(now);
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

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Optional<Instant> getUsedAt() {
        return Optional.ofNullable(usedAt);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private static String requireTokenHash(String tokenHash) {
        if (tokenHash == null || !tokenHash.matches("^[a-f0-9]{64}$")) {
            throw new IllegalArgumentException("tokenHash must be a 64-char lowercase SHA-256 hex");
        }
        return tokenHash;
    }
}
