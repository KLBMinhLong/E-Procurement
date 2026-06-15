package com.eprocure.admin.application.usecase;

import com.eprocure.admin.application.port.in.DeactivateCatalogCategoryCommand;
import com.eprocure.admin.application.port.out.ProcurementCatalogAdminPort;
import com.eprocure.admin.application.service.CatalogCategoryAdminView;
import com.eprocure.admin.application.service.IdempotencyGuard;
import com.eprocure.admin.common.util.LogMaskingUtil;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeactivateCatalogCategoryUseCase {
    private static final Logger log = LogManager.getLogger(DeactivateCatalogCategoryUseCase.class);

    private final ProcurementCatalogAdminPort procurementCatalogAdminPort;
    private final IdempotencyGuard idempotencyGuard;

    public DeactivateCatalogCategoryUseCase(
            ProcurementCatalogAdminPort procurementCatalogAdminPort,
            IdempotencyGuard idempotencyGuard) {
        this.procurementCatalogAdminPort = procurementCatalogAdminPort;
        this.idempotencyGuard = idempotencyGuard;
    }

    @Transactional
    public CatalogCategoryAdminView execute(DeactivateCatalogCategoryCommand command, String idempotencyKey) {
        UUID key = idempotencyGuard.verify(idempotencyKey);
        log.info("[ACTION] Start DeactivateCatalogCategory | actorId={} | code={}",
                LogMaskingUtil.maskId(command.actorId()),
                command.code());
        CatalogCategoryAdminView view = procurementCatalogAdminPort.deactivateCategory(command, key);
        log.info("[ACTION] Complete DeactivateCatalogCategory | actorId={} | code={}",
                LogMaskingUtil.maskId(command.actorId()),
                view.code());
        return view;
    }
}
