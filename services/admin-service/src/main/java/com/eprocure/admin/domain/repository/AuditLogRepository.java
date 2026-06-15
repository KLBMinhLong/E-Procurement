package com.eprocure.admin.domain.repository;

import com.eprocure.admin.application.service.PageResult;
import com.eprocure.admin.domain.model.AuditLogEntry;
import com.eprocure.admin.domain.model.AuditLogFilter;

public interface AuditLogRepository {
    PageResult<AuditLogEntry> findByFilter(AuditLogFilter filter);
}
