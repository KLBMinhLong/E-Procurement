package com.eprocure.finance.domain.repository;

import com.eprocure.finance.domain.model.PurchaseOrder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PurchaseOrderRepository {
    Optional<PurchaseOrder> findById(UUID poId);

    Optional<PurchaseOrder> findBySourceEventId(String sourceEventId);

    Optional<PurchaseOrder> findByRfqId(UUID rfqId);

    List<PurchaseOrder> findByFilter(PurchaseOrderFilter filter);

    long countByFilter(PurchaseOrderFilter filter);

    String nextPoNumber(int fiscalYear);

    void insert(PurchaseOrder purchaseOrder);

    void updateDraftDetails(PurchaseOrder purchaseOrder, UUID actorId);

    void updateActionState(PurchaseOrder purchaseOrder, UUID actorId);

    boolean existsProcessedEvent(String eventId);

    void markEventProcessed(String eventId, String topic, Integer partitionId, Long offsetValue, String handlerName);
}
