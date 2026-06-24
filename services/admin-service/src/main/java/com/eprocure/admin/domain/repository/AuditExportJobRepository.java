package com.eprocure.admin.domain.repository;

import com.eprocure.admin.domain.model.AuditExportJob;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditExportJobRepository {
    Optional<AuditExportJob> findByIdempotencyKey(UUID createdBy, UUID idempotencyKey);

    Optional<AuditExportJob> findByIdAndActorId(UUID jobId, UUID actorId);

    AuditExportJob saveQueued(AuditExportJob job);

    List<AuditExportJob> claimQueuedForProcessing(int limit, Instant claimedAt);

    void markCompleted(UUID jobId, String fileName, String storagePath, Instant completedAt, Instant expiresAt);

    void markFailed(UUID jobId, String failureReason, Instant failedAt);
}
