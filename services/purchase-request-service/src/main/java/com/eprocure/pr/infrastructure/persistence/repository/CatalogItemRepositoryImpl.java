package com.eprocure.pr.infrastructure.persistence.repository;

import com.eprocure.pr.domain.model.CatalogItem;
import com.eprocure.pr.domain.model.vo.Money;
import com.eprocure.pr.domain.repository.CatalogItemRepository;
import com.eprocure.pr.infrastructure.persistence.entity.CatalogItemDbEntity;
import com.eprocure.pr.infrastructure.persistence.mapper.CatalogItemMapper;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class CatalogItemRepositoryImpl implements CatalogItemRepository {

    private final CatalogItemMapper mapper;

    public CatalogItemRepositoryImpl(CatalogItemMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<CatalogItem> search(String query, String categoryCode, int offset, int limit) {
        return mapper.search(query, categoryCode, offset, limit).stream()
                .map(this::mapToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long count(String query, String categoryCode) {
        return mapper.count(query, categoryCode);
    }

    private CatalogItem mapToDomain(CatalogItemDbEntity entity) {
        Money unitPrice = new Money(entity.getUnitPrice(), entity.getCurrency() != null ? entity.getCurrency() : "VND");

        CatalogItem item = CatalogItem.reconstitute(
                entity.getId(),
                entity.getItemCode(),
                entity.getName(),
                entity.getDescription(),
                entity.getCategoryCode(),
                entity.getUnit(),
                unitPrice,
                entity.getPreferredVendorId(),
                entity.getReorderPoint(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.isDeleted(),
                entity.getDeletedAt(),
                entity.getDeletedBy()
        );

        if (entity.getQuantityOnHand() != null) {
            item.setQuantityOnHand(entity.getQuantityOnHand());
        }

        return item;
    }
}
