package com.eprocure.iam.domain.repository;

import com.eprocure.iam.domain.model.SessionRecord;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepository {
    void save(SessionRecord sessionRecord);

    void revokeActiveByUserId(UUID userId, UUID revokedBy, Instant revokedAt);

    void revokeByTokenHash(String tokenHash, UUID revokedBy, Instant revokedAt);

    Optional<SessionRecord> findActiveByTokenHash(String tokenHash, Instant now);
}
