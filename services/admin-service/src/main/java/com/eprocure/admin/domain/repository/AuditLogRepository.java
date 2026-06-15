package com.eprocure.admin.domain.repository;

import com.eprocure.admin.application.service.PageResult;
import com.eprocure.admin.domain.model.AuditLogEntry;
import com.eprocure.admin.domain.model.AuditLogFilter;
import java.util.List;

public interface AuditLogRepository {
    PageResult<AuditLogEntry> findByFilter(AuditLogFilter filter);

    List<AuditLogEntry> findForExport(AuditLogFilter filter, int limit);
}
