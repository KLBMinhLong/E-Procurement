package com.eprocure.admin.application.usecase;

import com.eprocure.admin.application.port.out.ProcurementCatalogAdminPort;
import com.eprocure.admin.application.service.CatalogCategoryAdminView;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListCatalogCategoriesUseCase {
    private static final Logger log = LogManager.getLogger(ListCatalogCategoriesUseCase.class);

    private final ProcurementCatalogAdminPort procurementCatalogAdminPort;

    public ListCatalogCategoriesUseCase(ProcurementCatalogAdminPort procurementCatalogAdminPort) {
        this.procurementCatalogAdminPort = procurementCatalogAdminPort;
    }

    @Transactional(readOnly = true)
    public List<CatalogCategoryAdminView> execute(boolean includeInactive) {
        log.info("[ACTION] Start ListCatalogCategories | includeInactive={}", includeInactive);
        List<CatalogCategoryAdminView> result = procurementCatalogAdminPort.listCategories(includeInactive);
        log.info("[ACTION] Complete ListCatalogCategories | count={}", result.size());
        return result;
    }
}
