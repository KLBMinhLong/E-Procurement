package com.eprocure.admin.application.port.out;

import com.eprocure.admin.application.port.in.DeactivateCatalogCategoryCommand;
import com.eprocure.admin.application.port.in.ManageCatalogCategoryCommand;
import com.eprocure.admin.application.service.CatalogCategoryAdminView;
import java.util.List;
import java.util.UUID;

public interface ProcurementCatalogAdminPort {
    List<CatalogCategoryAdminView> listCategories(boolean includeInactive);

    CatalogCategoryAdminView createCategory(ManageCatalogCategoryCommand command, UUID idempotencyKey);

    CatalogCategoryAdminView updateCategory(ManageCatalogCategoryCommand command, UUID idempotencyKey);

    CatalogCategoryAdminView deactivateCategory(DeactivateCatalogCategoryCommand command, UUID idempotencyKey);
}
