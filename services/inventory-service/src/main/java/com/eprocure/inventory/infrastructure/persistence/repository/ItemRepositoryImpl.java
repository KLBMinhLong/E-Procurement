package com.eprocure.inventory.infrastructure.persistence.repository;

import com.eprocure.inventory.domain.model.CatalogItemMutationRequest;
import com.eprocure.inventory.domain.model.Item;
import com.eprocure.inventory.domain.repository.ItemFilter;
import com.eprocure.inventory.domain.repository.ItemRepository;
import com.eprocure.inventory.infrastructure.persistence.entity.CatalogItemMutationRequestDbEntity;
import com.eprocure.inventory.infrastructure.persistence.entity.ItemDbEntity;
import com.eprocure.inventory.infrastructure.persistence.mapper.ItemMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class ItemRepositoryImpl implements ItemRepository {
    private final ItemMapper itemMapper;
    private final ObjectMapper domainObjectMapper;

    public ItemRepositoryImpl(
            ItemMapper itemMapper,
            @Qualifier("domainObjectMapper") ObjectMapper domainObjectMapper) {
        this.itemMapper = itemMapper;
        this.domainObjectMapper = domainObjectMapper;
    }

    @Override
    public Optional<Item> findByCode(String itemCode) {
        return itemMapper.findByCode(normalizeItemCode(itemCode))
                .map(entity -> domainObjectMapper.convertValue(entity, Item.class));
    }

    @Override
    public Optional<CatalogItemMutationRequest> findMutationRequestByIdempotencyKey(UUID idempotencyKey) {
        return itemMapper.findMutationRequestByIdempotencyKey(idempotencyKey)
                .map(entity -> domainObjectMapper.convertValue(entity, CatalogItemMutationRequest.class));
    }

    @Override
    public List<Item> findByFilter(ItemFilter filter) {
        return itemMapper.findByFilter(filter).stream()
                .map(entity -> domainObjectMapper.convertValue(entity, Item.class))
                .toList();
    }

    @Override
    public long countByFilter(ItemFilter filter) {
        return itemMapper.countByFilter(filter);
    }

    @Override
    public List<StockSummary> findStockSummary(String itemCode) {
        return itemMapper.findStockSummary(normalizeItemCode(itemCode)).stream()
                .map(entity -> domainObjectMapper.convertValue(entity, StockSummary.class))
                .toList();
    }

    @Override
    public boolean existsActiveCode(String itemCode) {
        return itemMapper.existsActiveCode(normalizeItemCode(itemCode));
    }

    @Override
    public void insert(Item item) {
        itemMapper.insert(domainObjectMapper.convertValue(item, ItemDbEntity.class));
    }

    @Override
    public boolean update(Item item) {
        return itemMapper.update(domainObjectMapper.convertValue(item, ItemDbEntity.class)) > 0;
    }

    @Override
    public void insertMutationRequest(CatalogItemMutationRequest request) {
        itemMapper.insertMutationRequest(
                domainObjectMapper.convertValue(request, CatalogItemMutationRequestDbEntity.class));
    }

    private String normalizeItemCode(String itemCode) {
        return itemCode == null ? null : itemCode.trim().toUpperCase();
    }
}
