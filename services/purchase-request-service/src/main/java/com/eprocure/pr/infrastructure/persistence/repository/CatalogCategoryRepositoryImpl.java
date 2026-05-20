package com.eprocure.pr.infrastructure.persistence.repository;

import com.eprocure.pr.domain.model.CatalogCategory;
import com.eprocure.pr.domain.model.vo.Money;
import com.eprocure.pr.domain.repository.CatalogCategoryRepository;
import com.eprocure.pr.infrastructure.persistence.entity.CatalogCategoryDbEntity;
import com.eprocure.pr.infrastructure.persistence.mapper.CatalogCategoryMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class CatalogCategoryRepositoryImpl implements CatalogCategoryRepository {

    private final CatalogCategoryMapper mapper;

    public CatalogCategoryRepositoryImpl(CatalogCategoryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<CatalogCategory> findAll() {
        List<CatalogCategoryDbEntity> entities = mapper.findAll();
        
        Map<String, CatalogCategory> categoryMap = new HashMap<>();
        List<CatalogCategory> roots = new ArrayList<>();

        for (CatalogCategoryDbEntity entity : entities) {
            CatalogCategory category = mapToDomain(entity);
            categoryMap.put(category.getCode(), category);
        }

        for (CatalogCategory category : categoryMap.values()) {
            if (category.getParentCode().isPresent()) {
                CatalogCategory parent = categoryMap.get(category.getParentCode().get());
                if (parent != null) {
                    parent.addChild(category);
                } else {
                    roots.add(category); // fallback
                }
            } else {
                roots.add(category);
            }
        }

        return roots;
    }

    private CatalogCategory mapToDomain(CatalogCategoryDbEntity entity) {
        Money requiresRfqAbove = null;
        if (entity.getRequiresRfqAbove() != null) {
            requiresRfqAbove = new Money(entity.getRequiresRfqAbove(), entity.getCurrency() != null ? entity.getCurrency() : "VND");
        }

        return CatalogCategory.reconstitute(
                entity.getCode(),
                entity.getName(),
                entity.getParentCode(),
                entity.isRequiresSpecialApproval(),
                entity.getSpecialApproverRole(),
                requiresRfqAbove,
                entity.isCapex(),
                entity.getCreatedAt(),
                entity.getUpdatedAt() != null ? entity.getUpdatedAt() : entity.getCreatedAt(),
                entity.isDeleted()
        );
    }
}
