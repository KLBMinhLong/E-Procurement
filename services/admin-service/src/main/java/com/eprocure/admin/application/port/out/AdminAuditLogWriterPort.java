package com.eprocure.admin.application.port.out;

import com.eprocure.admin.application.service.AdminAuditContext;
import com.eprocure.admin.application.service.CatalogCategoryAdminView;
import com.eprocure.admin.application.service.DepartmentAdminView;
import com.eprocure.admin.domain.model.AdminConfigAction;
import com.eprocure.admin.domain.model.AuditExportJob;
import java.util.UUID;

public interface AdminAuditLogWriterPort {
    void recordConfigAction(AdminConfigAction action, AdminAuditContext context);

    void recordSessionInvalidation(UUID sessionId, AdminAuditContext context);

    void recordCatalogCategoryMutation(String auditAction, CatalogCategoryAdminView category, AdminAuditContext context);

    void recordDepartmentMutation(String auditAction, DepartmentAdminView department, AdminAuditContext context);

    void recordAuditExportRequest(AuditExportJob job, AdminAuditContext context);
}
