package com.eprocure.inventory.domain.repository;

import com.eprocure.inventory.domain.model.CatalogItemMutationRequest;
import com.eprocure.inventory.domain.model.Item;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemRepository {
    Optional<Item> findByCode(String itemCode);

    Optional<CatalogItemMutationRequest> findMutationRequestByIdempotencyKey(UUID idempotencyKey);

    List<Item> findByFilter(ItemFilter filter);

    long countByFilter(ItemFilter filter);

    List<StockSummary> findStockSummary(String itemCode);

    boolean existsActiveCode(String itemCode);

    void insert(Item item);

    boolean update(Item item);

    void insertMutationRequest(CatalogItemMutationRequest request);

    record StockSummary(UUID warehouseId, String warehouseName, java.math.BigDecimal quantityOnHand, String unit) {
    }
}
