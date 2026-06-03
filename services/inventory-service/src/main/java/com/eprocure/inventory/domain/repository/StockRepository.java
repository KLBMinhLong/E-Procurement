package com.eprocure.inventory.domain.repository;

import com.eprocure.inventory.domain.model.StockEntry;
import com.eprocure.inventory.domain.model.StockIssueOutRequest;
import com.eprocure.inventory.domain.model.StockMovement;
import com.eprocure.inventory.domain.model.StockMovementHistory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockRepository {
    boolean existsActiveItem(String itemCode);

    boolean existsActiveWarehouse(UUID warehouseId);

    List<StockEntry> findStockEntries(StockEntryFilter filter);

    long countStockEntries(StockEntryFilter filter);

    List<StockMovementHistory> findMovements(StockMovementFilter filter);

    long countMovements(StockMovementFilter filter);

    Optional<StockIssueOutRequest> findIssueOutRequestByIdempotencyKey(UUID idempotencyKey);

    void insertIssueOutRequest(StockIssueOutRequest issueOutRequest);

    Optional<BigDecimal> issueStock(
            String itemCode,
            UUID warehouseId,
            BigDecimal quantity,
            String unit,
            UUID actorId,
            Instant occurredAt);

    void insertStockMovement(StockMovement stockMovement);

    List<StockMovementHistory> findMovementsBySource(String sourceRefType, UUID sourceRefId);
}
