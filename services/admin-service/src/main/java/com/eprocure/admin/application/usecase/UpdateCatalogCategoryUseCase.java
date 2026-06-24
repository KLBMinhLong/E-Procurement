package com.eprocure.admin.application.usecase;

import com.eprocure.admin.application.port.in.ManageCatalogCategoryCommand;
import com.eprocure.admin.application.port.out.AdminAuditLogWriterPort;
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
public class UpdateCatalogCategoryUseCase {
    private static final Logger log = LogManager.getLogger(UpdateCatalogCategoryUseCase.class);

    private final ProcurementCatalogAdminPort procurementCatalogAdminPort;
    private final AdminAuditLogWriterPort auditLogWriter;
    private final IdempotencyGuard idempotencyGuard;

    public UpdateCatalogCategoryUseCase(
            ProcurementCatalogAdminPort procurementCatalogAdminPort,
            AdminAuditLogWriterPort auditLogWriter,
            IdempotencyGuard idempotencyGuard) {
        this.procurementCatalogAdminPort = procurementCatalogAdminPort;
        this.auditLogWriter = auditLogWriter;
        this.idempotencyGuard = idempotencyGuard;
    }

    @Transactional
    public CatalogCategoryAdminView execute(ManageCatalogCategoryCommand command, String idempotencyKey) {
        UUID key = idempotencyGuard.verify(idempotencyKey);
        log.info("[ACTION] Start UpdateCatalogCategory | actorId={} | code={}",
                LogMaskingUtil.maskId(command.actorId()),
                command.code());
        CatalogCategoryAdminView view = procurementCatalogAdminPort.updateCategory(command, key);
        auditLogWriter.recordCatalogCategoryMutation("CATALOG_CATEGORY.UPDATED", view, command.auditContext());
        log.info("[ACTION] Complete UpdateCatalogCategory | actorId={} | code={}",
                LogMaskingUtil.maskId(command.actorId()),
                view.code());
        return view;
    }
}
