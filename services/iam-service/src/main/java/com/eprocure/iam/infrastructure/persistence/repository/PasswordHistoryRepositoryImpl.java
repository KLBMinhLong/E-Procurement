package com.eprocure.iam.infrastructure.persistence.repository;

import com.eprocure.iam.domain.repository.PasswordHistoryRepository;
import com.eprocure.iam.infrastructure.persistence.mapper.PasswordHistoryMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class PasswordHistoryRepositoryImpl implements PasswordHistoryRepository {
    private final PasswordHistoryMapper passwordHistoryMapper;

    public PasswordHistoryRepositoryImpl(PasswordHistoryMapper passwordHistoryMapper) {
        this.passwordHistoryMapper = passwordHistoryMapper;
    }

    @Override
    public List<String> findRecentHashesByUserId(UUID userId, int limit) {
        return List.copyOf(Optional.ofNullable(passwordHistoryMapper.findRecentHashesByUserId(userId, limit))
                .orElseGet(List::of));
    }

    @Override
    public void save(UUID userId, String passwordHash, UUID actorId, Instant changedAt) {
        passwordHistoryMapper.insert(userId, passwordHash, actorId, changedAt);
    }
}
