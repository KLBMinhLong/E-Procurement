package com.eprocure.pr.domain.repository;

import com.eprocure.pr.domain.model.PurchaseRequest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PurchaseRequestRepository {
    void save(PurchaseRequest purchaseRequest);

    void update(PurchaseRequest purchaseRequest);

    /**
     * Update root PR fields AND replace all line items atomically.
     * Used by UpdatePR use case where line items may be completely replaced.
     */
    void updateWithLineItems(PurchaseRequest purchaseRequest);

    Optional<PurchaseRequest> findById(UUID id);

    Optional<PurchaseRequest> findByPrNumber(String prNumber);

    boolean existsByPrNumber(String prNumber);

    void softDelete(UUID id, UUID deletedBy, Instant deletedAt);

    /**
     * Filtered, paginated list — returns lightweight PR objects (line items not loaded).
     */
    List<PurchaseRequest> findByFilter(PurchaseRequestFilter filter);

    /**
     * Total count matching the same filter (for pagination meta).
     */
    long countByFilter(PurchaseRequestFilter filter);
}
