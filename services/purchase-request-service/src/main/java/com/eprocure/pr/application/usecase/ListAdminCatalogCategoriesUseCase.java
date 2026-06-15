package com.eprocure.pr.application.usecase;

import com.eprocure.pr.application.service.CatalogCategoryAdminView;
import com.eprocure.pr.domain.model.CatalogCategoryAdmin;
import com.eprocure.pr.domain.repository.CatalogCategoryRepository;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListAdminCatalogCategoriesUseCase {
    private static final Logger log = LogManager.getLogger(ListAdminCatalogCategoriesUseCase.class);

    private final CatalogCategoryRepository catalogCategoryRepository;

    public ListAdminCatalogCategoriesUseCase(CatalogCategoryRepository catalogCategoryRepository) {
        this.catalogCategoryRepository = catalogCategoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CatalogCategoryAdminView> execute(boolean includeInactive) {
        log.info("[ACTION] Start ListAdminCatalogCategories | includeInactive={}", includeInactive);
        List<CatalogCategoryAdminView> result = catalogCategoryRepository.findAdminCategories(includeInactive)
                .stream()
                .map(this::toView)
                .toList();
        log.info("[ACTION] Complete ListAdminCatalogCategories | count={}", result.size());
        return result;
    }

    private CatalogCategoryAdminView toView(CatalogCategoryAdmin category) {
        return new CatalogCategoryAdminView(
                category.code(),
                category.name(),
                category.parentCode().orElse(null),
                category.requiresSpecialApproval(),
                category.specialApproverRole().orElse(null),
                category.requiresRfqAbove().map(money -> money.amount().toPlainString()).orElse(null),
                category.capex(),
                category.itemCount(),
                category.deleted());
    }
}
