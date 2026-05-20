package com.eprocure.pr.application.usecase;

import com.eprocure.pr.domain.model.CatalogCategory;
import com.eprocure.pr.domain.repository.CatalogCategoryRepository;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetCatalogCategoriesUseCase {
    private static final Logger log = LogManager.getLogger(GetCatalogCategoriesUseCase.class);

    private final CatalogCategoryRepository categoryRepository;

    public GetCatalogCategoriesUseCase(CatalogCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CatalogCategory> getCategories() {
        log.info("[ACTION] Start GetCatalogCategories");
        List<CatalogCategory> categories = categoryRepository.findAll();
        log.info("[ACTION] Complete GetCatalogCategories | count={}", categories.size());
        return categories;
    }
}
