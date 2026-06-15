package com.eprocure.admin.presentation.response;

import com.eprocure.admin.domain.model.AuditExportJobStatus;
import java.util.UUID;

public record AuditExportJobResponse(UUID jobId, AuditExportJobStatus status) {
}
