package com.eprocure.pr.domain.repository;

import com.eprocure.pr.domain.model.CatalogCategoryAdmin;
import com.eprocure.pr.domain.model.CatalogCategory;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CatalogCategoryRepository {
    List<CatalogCategory> findAll();

    List<CatalogCategoryAdmin> findAdminCategories(boolean includeInactive);

    Optional<CatalogCategoryAdmin> findAdminByCode(String code);

    boolean existsByCode(String code);

    boolean existsActiveByCode(String code);

    long countActiveItems(String categoryCode);

    void create(CatalogCategory category, UUID actorId);

    void update(CatalogCategory category, UUID actorId);

    void deactivate(String code, UUID actorId, Instant deletedAt);
}
