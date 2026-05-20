package com.eprocure.pr.domain.repository;

import com.eprocure.pr.domain.model.CatalogCategory;
import java.util.List;

public interface CatalogCategoryRepository {
    List<CatalogCategory> findAll();
}
