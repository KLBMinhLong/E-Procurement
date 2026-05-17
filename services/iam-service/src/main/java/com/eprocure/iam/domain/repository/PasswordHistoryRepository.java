package com.eprocure.iam.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PasswordHistoryRepository {
    List<String> findRecentHashesByUserId(UUID userId, int limit);

    void save(UUID userId, String passwordHash, UUID actorId, Instant changedAt);
}
