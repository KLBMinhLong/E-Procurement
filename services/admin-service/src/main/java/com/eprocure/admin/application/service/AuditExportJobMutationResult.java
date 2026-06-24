package com.eprocure.admin.application.service;

import com.eprocure.admin.domain.model.AuditExportJob;

public record AuditExportJobMutationResult(AuditExportJob job, boolean replayed) {
}
