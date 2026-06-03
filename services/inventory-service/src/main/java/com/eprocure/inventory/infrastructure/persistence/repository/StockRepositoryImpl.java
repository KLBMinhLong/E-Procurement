package com.eprocure.inventory.infrastructure.persistence.repository;

import com.eprocure.inventory.domain.model.StockEntry;
import com.eprocure.inventory.domain.model.StockIssueOutRequest;
import com.eprocure.inventory.domain.model.StockMovement;
import com.eprocure.inventory.domain.model.StockMovementHistory;
import com.eprocure.inventory.domain.repository.StockEntryFilter;
import com.eprocure.inventory.domain.repository.StockMovementFilter;
import com.eprocure.inventory.domain.repository.StockRepository;
import com.eprocure.inventory.infrastructure.persistence.entity.StockIssueOutRequestDbEntity;
import com.eprocure.inventory.infrastructure.persistence.entity.StockMovementDbEntity;
import com.eprocure.inventory.infrastructure.persistence.mapper.StockMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class StockRepositoryImpl implements StockRepository {
    private final StockMapper stockMapper;
    private final ObjectMapper domainObjectMapper;

    public StockRepositoryImpl(
            StockMapper stockMapper,
            @Qualifier("domainObjectMapper") ObjectMapper domainObjectMapper) {
        this.stockMapper = stockMapper;
        this.domainObjectMapper = domainObjectMapper;
    }

    @Override
    public boolean existsActiveItem(String itemCode) {
        return stockMapper.existsActiveItem(itemCode);
    }

    @Override
    public boolean existsActiveWarehouse(UUID warehouseId) {
        return stockMapper.existsActiveWarehouse(warehouseId);
    }

    @Override
    public List<StockEntry> findStockEntries(StockEntryFilter filter) {
        return stockMapper.findStockEntries(filter).stream()
                .map(entity -> domainObjectMapper.convertValue(entity, StockEntry.class))
                .toList();
    }

    @Override
    public long countStockEntries(StockEntryFilter filter) {
        return stockMapper.countStockEntries(filter);
    }

    @Override
    public List<StockMovementHistory> findMovements(StockMovementFilter filter) {
        return stockMapper.findMovements(filter).stream()
                .map(entity -> domainObjectMapper.convertValue(entity, StockMovementHistory.class))
                .toList();
    }

    @Override
    public long countMovements(StockMovementFilter filter) {
        return stockMapper.countMovements(filter);
    }

    @Override
    public Optional<StockIssueOutRequest> findIssueOutRequestByIdempotencyKey(UUID idempotencyKey) {
        return stockMapper.findIssueOutRequestByIdempotencyKey(idempotencyKey)
                .map(entity -> domainObjectMapper.convertValue(entity, StockIssueOutRequest.class));
    }

    @Override
    public void insertIssueOutRequest(StockIssueOutRequest issueOutRequest) {
        stockMapper.insertIssueOutRequest(
                domainObjectMapper.convertValue(issueOutRequest, StockIssueOutRequestDbEntity.class));
    }

    @Override
    public Optional<BigDecimal> issueStock(
            String itemCode,
            UUID warehouseId,
            BigDecimal quantity,
            String unit,
            UUID actorId,
            Instant occurredAt) {
        return stockMapper.issueStock(itemCode, warehouseId, quantity, unit, actorId, occurredAt);
    }

    @Override
    public void insertStockMovement(StockMovement stockMovement) {
        stockMapper.insertStockMovement(
                domainObjectMapper.convertValue(stockMovement, StockMovementDbEntity.class));
    }

    @Override
    public List<StockMovementHistory> findMovementsBySource(String sourceRefType, UUID sourceRefId) {
        return stockMapper.findMovementsBySource(sourceRefType, sourceRefId).stream()
                .map(entity -> domainObjectMapper.convertValue(entity, StockMovementHistory.class))
                .toList();
    }
}
