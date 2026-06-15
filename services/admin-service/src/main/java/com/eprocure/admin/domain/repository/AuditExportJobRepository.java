package com.eprocure.admin.domain.repository;

import com.eprocure.admin.domain.model.AuditExportJob;
import java.util.Optional;
import java.util.UUID;

public interface AuditExportJobRepository {
    Optional<AuditExportJob> findByIdempotencyKey(UUID createdBy, UUID idempotencyKey);

    AuditExportJob saveQueued(AuditExportJob job);
}
