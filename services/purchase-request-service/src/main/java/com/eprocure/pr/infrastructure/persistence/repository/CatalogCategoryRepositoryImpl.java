package com.eprocure.pr.infrastructure.persistence.repository;

import com.eprocure.pr.domain.model.CatalogCategory;
import com.eprocure.pr.domain.model.CatalogCategoryAdmin;
import com.eprocure.pr.domain.model.vo.Money;
import com.eprocure.pr.domain.repository.CatalogCategoryRepository;
import com.eprocure.pr.infrastructure.persistence.entity.CatalogCategoryAdminDbEntity;
import com.eprocure.pr.infrastructure.persistence.entity.CatalogCategoryDbEntity;
import com.eprocure.pr.infrastructure.persistence.mapper.CatalogCategoryMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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

    @Override
    public List<CatalogCategoryAdmin> findAdminCategories(boolean includeInactive) {
        return mapper.findAdminCategories(includeInactive).stream()
                .map(this::mapToAdmin)
                .toList();
    }

    @Override
    public Optional<CatalogCategoryAdmin> findAdminByCode(String code) {
        return Optional.ofNullable(mapper.findAdminByCode(code)).map(this::mapToAdmin);
    }

    @Override
    public boolean existsByCode(String code) {
        return mapper.existsByCode(code);
    }

    @Override
    public boolean existsActiveByCode(String code) {
        return mapper.existsActiveByCode(code);
    }

    @Override
    public long countActiveItems(String categoryCode) {
        return mapper.countActiveItems(categoryCode);
    }

    @Override
    public void create(CatalogCategory category, UUID actorId) {
        mapper.insert(
                category.getCode(),
                category.getName(),
                category.getParentCode().orElse(null),
                category.isRequiresSpecialApproval(),
                category.getSpecialApproverRole().orElse(null),
                category.getRequiresRfqAbove().map(Money::amount).orElse(null),
                category.isCapex(),
                actorId);
    }

    @Override
    public void update(CatalogCategory category, UUID actorId) {
        mapper.update(
                category.getCode(),
                category.getName(),
                category.getParentCode().orElse(null),
                category.isRequiresSpecialApproval(),
                category.getSpecialApproverRole().orElse(null),
                category.getRequiresRfqAbove().map(Money::amount).orElse(null),
                category.isCapex(),
                actorId);
    }

    @Override
    public void deactivate(String code, UUID actorId, Instant deletedAt) {
        mapper.deactivate(code, actorId, deletedAt);
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

    private CatalogCategoryAdmin mapToAdmin(CatalogCategoryAdminDbEntity entity) {
        Money requiresRfqAbove = null;
        if (entity.getRequiresRfqAbove() != null) {
            requiresRfqAbove = new Money(entity.getRequiresRfqAbove(), entity.getCurrency());
        }

        return new CatalogCategoryAdmin(
                entity.getCode(),
                entity.getName(),
                Optional.ofNullable(entity.getParentCode()),
                entity.isRequiresSpecialApproval(),
                Optional.ofNullable(entity.getSpecialApproverRole()),
                Optional.ofNullable(requiresRfqAbove),
                entity.isCapex(),
                entity.getItemCount(),
                entity.isDeleted());
    }
}
