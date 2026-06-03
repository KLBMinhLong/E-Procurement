package com.eprocure.inventory.domain.repository;

import com.eprocure.inventory.domain.model.PurchaseOrderSnapshot;
import java.util.Optional;
import java.util.UUID;

public interface PurchaseOrderSnapshotRepository {
    Optional<PurchaseOrderSnapshot> findByPoId(UUID poId);

    Optional<PurchaseOrderSnapshot> findBySourceEventId(String sourceEventId);

    boolean existsProcessedEvent(String eventId);

    void insert(PurchaseOrderSnapshot purchaseOrderSnapshot);

    void markEventProcessed(String eventId, String topic, Integer partitionId, Long offsetValue, String handlerName);
}
