package com.eprocure.finance.infrastructure.persistence.repository;

import com.eprocure.finance.domain.model.GoodsReceiptSnapshot;
import com.eprocure.finance.domain.repository.GoodsReceiptSnapshotRepository;
import com.eprocure.finance.infrastructure.persistence.entity.GoodsReceiptLineSnapshotDbEntity;
import com.eprocure.finance.infrastructure.persistence.entity.GoodsReceiptSnapshotDbEntity;
import com.eprocure.finance.infrastructure.persistence.mapper.GoodsReceiptSnapshotMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class GoodsReceiptSnapshotRepositoryImpl implements GoodsReceiptSnapshotRepository {
    private final GoodsReceiptSnapshotMapper mapper;

    public GoodsReceiptSnapshotRepositoryImpl(GoodsReceiptSnapshotMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean existsProcessedEvent(String eventId) {
        return mapper.existsProcessedEvent(eventId);
    }

    @Override
    public void upsert(GoodsReceiptSnapshot snapshot) {
        mapper.upsertHeader(GoodsReceiptSnapshotDbEntity.from(snapshot));
        snapshot.lineItems().stream()
                .map(lineItem -> GoodsReceiptLineSnapshotDbEntity.from(snapshot.id(), lineItem))
                .forEach(entity -> mapper.upsertLineItem(entity, snapshot.warehouseKeeperId()));
    }

    @Override
    public void markEventProcessed(String eventId, String topic, Integer partitionId, Long offsetValue, String handlerName) {
        mapper.markEventProcessed(eventId, topic, partitionId, offsetValue, handlerName);
    }

    @Override
    public List<ReceivedQuantity> findReceivedQuantitiesByPoId(UUID poId) {
        return mapper.findReceivedQuantitiesByPoId(poId).stream()
                .map(entity -> new ReceivedQuantity(entity.getPoLineItemId(), entity.getReceivedQuantity()))
                .toList();
    }
}
