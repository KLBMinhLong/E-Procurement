package com.eprocure.analytics.domain.repository;

import com.eprocure.analytics.domain.model.report.ReportJob;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportJobRepository {
    Optional<ReportJob> findByIdempotencyKey(UUID actorId, UUID idempotencyKey);

    Optional<ReportJob> findByIdAndActorId(UUID jobId, UUID actorId);

    void saveQueued(ReportJob job, JsonNode filters);

    List<ReportJob> claimQueuedForProcessing(int limit, Instant claimedAt);

    void markCompleted(UUID jobId, String downloadUrl, String storagePath, Instant completedAt, Instant expiresAt);

    void markFailed(UUID jobId, String failureReason, Instant failedAt);
}
