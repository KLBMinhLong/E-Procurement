package com.eprocure.admin.application.port.out;

import com.eprocure.admin.application.service.AdminAuditContext;
import com.eprocure.admin.domain.model.AdminConfigAction;
import java.util.UUID;

public interface AdminAuditLogWriterPort {
    void recordConfigAction(AdminConfigAction action, AdminAuditContext context);

    void recordSessionInvalidation(UUID sessionId, AdminAuditContext context);
}
