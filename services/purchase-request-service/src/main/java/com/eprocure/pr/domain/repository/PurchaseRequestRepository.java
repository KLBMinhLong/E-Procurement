package com.eprocure.pr.domain.repository;

import com.eprocure.pr.domain.model.PurchaseRequest;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PurchaseRequestRepository {
    void save(PurchaseRequest purchaseRequest);

    void update(PurchaseRequest purchaseRequest);

    Optional<PurchaseRequest> findById(UUID id);

    Optional<PurchaseRequest> findByPrNumber(String prNumber);

    boolean existsByPrNumber(String prNumber);

    void softDelete(UUID id, UUID deletedBy, Instant deletedAt);
}
