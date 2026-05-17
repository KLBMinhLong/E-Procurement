package com.eprocure.iam.infrastructure.persistence.repository;

import com.eprocure.iam.domain.model.SessionRecord;
import com.eprocure.iam.domain.repository.SessionRepository;
import com.eprocure.iam.infrastructure.persistence.entity.SessionDbEntity;
import com.eprocure.iam.infrastructure.persistence.mapper.SessionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class SessionRepositoryImpl implements SessionRepository {
    private final SessionMapper sessionMapper;
    private final ObjectMapper objectMapper;

    public SessionRepositoryImpl(SessionMapper sessionMapper, @Qualifier("domainObjectMapper") ObjectMapper objectMapper) {
        this.sessionMapper = sessionMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(SessionRecord sessionRecord) {
        sessionMapper.save(objectMapper.convertValue(sessionRecord, SessionDbEntity.class));
    }

    @Override
    public void revokeActiveByUserId(UUID userId, UUID revokedBy, Instant revokedAt) {
        sessionMapper.revokeActiveByUserId(userId, revokedBy, revokedAt);
    }

    @Override
    public void revokeByTokenHash(String tokenHash, UUID revokedBy, Instant revokedAt) {
        sessionMapper.revokeByTokenHash(tokenHash, revokedBy, revokedAt);
    }

    @Override
    public Optional<SessionRecord> findActiveByTokenHash(String tokenHash, Instant now) {
        return Optional.ofNullable(sessionMapper.findActiveByTokenHash(tokenHash, now)).map(this::toDomain);
    }

    @Override
    public List<String> findActiveTokenHashesByUserId(UUID userId, Instant now) {
        return List.copyOf(Optional.ofNullable(sessionMapper.findActiveTokenHashesByUserId(userId, now))
                .orElseGet(List::of));
    }

    private SessionRecord toDomain(SessionDbEntity entity) {
        return objectMapper.convertValue(entity, SessionRecord.class);
    }
}
