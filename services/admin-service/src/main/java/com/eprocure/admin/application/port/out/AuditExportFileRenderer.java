package com.eprocure.admin.application.port.out;

import com.eprocure.admin.domain.model.AuditExportJob;
import com.eprocure.admin.domain.model.AuditLogEntry;
import java.util.List;

public interface AuditExportFileRenderer {
    RenderedAuditExport render(AuditExportJob job, List<AuditLogEntry> entries);
}
