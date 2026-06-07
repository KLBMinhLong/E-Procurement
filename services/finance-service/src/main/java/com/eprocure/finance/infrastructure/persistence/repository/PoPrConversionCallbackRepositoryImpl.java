package com.eprocure.finance.infrastructure.persistence.repository;

import com.eprocure.finance.domain.model.PoPrConversionCallback;
import com.eprocure.finance.domain.model.PoPrConversionCallbackStatus;
import com.eprocure.finance.domain.repository.PoPrConversionCallbackRepository;
import com.eprocure.finance.infrastructure.persistence.entity.PoPrConversionCallbackDbEntity;
import com.eprocure.finance.infrastructure.persistence.mapper.PoPrConversionCallbackMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class PoPrConversionCallbackRepositoryImpl implements PoPrConversionCallbackRepository {
    private final PoPrConversionCallbackMapper mapper;

    public PoPrConversionCallbackRepositoryImpl(PoPrConversionCallbackMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void insert(PoPrConversionCallback callback) {
        mapper.insert(PoPrConversionCallbackDbEntity.from(callback));
    }

    @Override
    public Optional<PoPrConversionCallback> findById(UUID callbackId) {
        return mapper.findById(callbackId).map(PoPrConversionCallbackDbEntity::toDomain);
    }

    @Override
    public Optional<PoPrConversionCallback> findByPoId(UUID poId) {
        return mapper.findByPoId(poId).map(PoPrConversionCallbackDbEntity::toDomain);
    }

    @Override
    public Optional<PoPrConversionCallbackStatus> findStatusByPoId(UUID poId) {
        return mapper.findStatusByPoId(poId);
    }

    @Override
    public Map<UUID, PoPrConversionCallbackStatus> findStatusesByPoIds(List<UUID> poIds) {
        if (poIds.isEmpty()) {
            return Map.of();
        }
        return mapper.findByPoIds(poIds).stream()
                .collect(Collectors.toUnmodifiableMap(
                        PoPrConversionCallbackDbEntity::getPoId,
                        PoPrConversionCallbackDbEntity::getStatus));
    }

    @Override
    public List<PoPrConversionCallback> findDispatchable(Instant now, int limit) {
        return mapper.findDispatchable(now, limit).stream()
                .map(PoPrConversionCallbackDbEntity::toDomain)
                .toList();
    }

    @Override
    public void markDelivered(UUID callbackId, Instant deliveredAt) {
        mapper.markDelivered(callbackId, deliveredAt);
    }

    @Override
    public void markRetryable(UUID callbackId, int attempts, Instant nextRetryAt, String lastError) {
        mapper.markRetryable(callbackId, attempts, nextRetryAt, lastError);
    }

    @Override
    public void markExhausted(UUID callbackId, int attempts, String lastError) {
        mapper.markExhausted(callbackId, attempts, lastError);
    }
}
