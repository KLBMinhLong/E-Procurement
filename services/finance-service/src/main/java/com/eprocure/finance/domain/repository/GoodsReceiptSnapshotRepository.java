package com.eprocure.finance.domain.repository;

import com.eprocure.finance.domain.model.GoodsReceiptSnapshot;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface GoodsReceiptSnapshotRepository {
    boolean existsProcessedEvent(String eventId);

    void upsert(GoodsReceiptSnapshot snapshot);

    void markEventProcessed(String eventId, String topic, Integer partitionId, Long offsetValue, String handlerName);

    List<ReceivedQuantity> findReceivedQuantitiesByPoId(UUID poId);

    record ReceivedQuantity(UUID poLineItemId, BigDecimal receivedQuantity) {
    }
}
