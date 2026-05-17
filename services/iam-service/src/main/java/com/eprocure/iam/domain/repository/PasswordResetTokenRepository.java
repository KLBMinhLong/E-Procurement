package com.eprocure.iam.domain.repository;

import com.eprocure.iam.domain.model.PasswordResetToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository {
    void save(PasswordResetToken token, UUID actorId);

    Optional<PasswordResetToken> findActiveByTokenHash(String tokenHash, Instant now);

    void markUsed(UUID tokenId, UUID actorId, Instant usedAt);

    void revokeActiveByUserId(UUID userId, UUID actorId, Instant revokedAt);
}
