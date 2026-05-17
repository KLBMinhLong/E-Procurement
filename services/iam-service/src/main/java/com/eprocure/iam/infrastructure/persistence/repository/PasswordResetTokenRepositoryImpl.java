package com.eprocure.iam.infrastructure.persistence.repository;

import com.eprocure.iam.domain.model.PasswordResetToken;
import com.eprocure.iam.domain.repository.PasswordResetTokenRepository;
import com.eprocure.iam.infrastructure.persistence.entity.PasswordResetTokenDbEntity;
import com.eprocure.iam.infrastructure.persistence.mapper.PasswordResetTokenMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class PasswordResetTokenRepositoryImpl implements PasswordResetTokenRepository {
    private final PasswordResetTokenMapper passwordResetTokenMapper;
    private final ObjectMapper objectMapper;

    public PasswordResetTokenRepositoryImpl(
            PasswordResetTokenMapper passwordResetTokenMapper,
            @Qualifier("domainObjectMapper") ObjectMapper objectMapper) {
        this.passwordResetTokenMapper = passwordResetTokenMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(PasswordResetToken token, UUID actorId) {
        passwordResetTokenMapper.insert(objectMapper.convertValue(token, PasswordResetTokenDbEntity.class), actorId);
    }

    @Override
    public Optional<PasswordResetToken> findActiveByTokenHash(String tokenHash, Instant now) {
        return Optional.ofNullable(passwordResetTokenMapper.findActiveByTokenHash(tokenHash, now))
                .map(entity -> objectMapper.convertValue(entity, PasswordResetToken.class));
    }

    @Override
    public void markUsed(UUID tokenId, UUID actorId, Instant usedAt) {
        passwordResetTokenMapper.markUsed(tokenId, actorId, usedAt);
    }

    @Override
    public void revokeActiveByUserId(UUID userId, UUID actorId, Instant revokedAt) {
        passwordResetTokenMapper.revokeActiveByUserId(userId, actorId, revokedAt);
    }
}
