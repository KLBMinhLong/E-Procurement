package com.eprocure.finance.domain.repository;

import com.eprocure.finance.domain.model.PoPrConversionCallback;
import com.eprocure.finance.domain.model.PoPrConversionCallbackStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface PoPrConversionCallbackRepository {
    void insert(PoPrConversionCallback callback);

    Optional<PoPrConversionCallback> findById(UUID callbackId);

    Optional<PoPrConversionCallback> findByPoId(UUID poId);

    Optional<PoPrConversionCallbackStatus> findStatusByPoId(UUID poId);

    Map<UUID, PoPrConversionCallbackStatus> findStatusesByPoIds(List<UUID> poIds);

    List<PoPrConversionCallback> findDispatchable(Instant now, int limit);

    void markDelivered(UUID callbackId, Instant deliveredAt);

    void markRetryable(UUID callbackId, int attempts, Instant nextRetryAt, String lastError);

    void markExhausted(UUID callbackId, int attempts, String lastError);
}
