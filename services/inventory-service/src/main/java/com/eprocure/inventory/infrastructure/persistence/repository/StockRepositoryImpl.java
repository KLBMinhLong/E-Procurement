package com.eprocure.inventory.infrastructure.persistence.repository;

import com.eprocure.inventory.domain.model.StockEntry;
import com.eprocure.inventory.domain.model.StockMovementHistory;
import com.eprocure.inventory.domain.repository.StockEntryFilter;
import com.eprocure.inventory.domain.repository.StockMovementFilter;
import com.eprocure.inventory.domain.repository.StockRepository;
import com.eprocure.inventory.infrastructure.persistence.mapper.StockMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
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
}
