package com.eprocure.inventory.domain.repository;

import com.eprocure.inventory.domain.model.StockEntry;
import com.eprocure.inventory.domain.model.StockMovementHistory;
import java.util.List;
import java.util.UUID;

public interface StockRepository {
    boolean existsActiveItem(String itemCode);

    boolean existsActiveWarehouse(UUID warehouseId);

    List<StockEntry> findStockEntries(StockEntryFilter filter);

    long countStockEntries(StockEntryFilter filter);

    List<StockMovementHistory> findMovements(StockMovementFilter filter);

    long countMovements(StockMovementFilter filter);
}
