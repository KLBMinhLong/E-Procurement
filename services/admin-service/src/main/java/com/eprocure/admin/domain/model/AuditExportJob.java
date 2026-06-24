package com.eprocure.admin.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record AuditExportJob(
        UUID id,
        AuditExportJobStatus status,
        Instant fromTime,
        Instant toTime,
        Optional<UUID> filterActorId,
        Optional<String> entityType,
        Optional<String> action,
        Optional<String> fileName,
        Optional<String> storagePath,
        Optional<String> failureReason,
        UUID idempotencyKey,
        Instant requestedAt,
        Optional<Instant> completedAt,
        Optional<Instant> expiresAt,
        UUID createdBy) {

    public AuditExportJob {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(fromTime, "fromTime must not be null");
        Objects.requireNonNull(toTime, "toTime must not be null");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        Objects.requireNonNull(requestedAt, "requestedAt must not be null");
        Objects.requireNonNull(createdBy, "createdBy must not be null");
        filterActorId = filterActorId == null ? Optional.empty() : filterActorId;
        entityType = normalize(entityType);
        action = normalize(action);
        fileName = normalize(fileName);
        storagePath = normalize(storagePath);
        failureReason = normalize(failureReason);
        completedAt = completedAt == null ? Optional.empty() : completedAt;
        expiresAt = expiresAt == null ? Optional.empty() : expiresAt;
    }

    public static AuditExportJob queued(
            UUID id,
            Instant fromTime,
            Instant toTime,
            Optional<UUID> filterActorId,
            Optional<String> entityType,
            Optional<String> action,
            UUID idempotencyKey,
            Instant requestedAt,
            Instant expiresAt,
            UUID createdBy) {
        return new AuditExportJob(
                id,
                AuditExportJobStatus.QUEUED,
                fromTime,
                toTime,
                filterActorId,
                entityType,
                action,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                idempotencyKey,
                requestedAt,
                Optional.empty(),
                Optional.of(expiresAt),
                createdBy);
    }

    private static Optional<String> normalize(Optional<String> value) {
        if (value == null || value.isEmpty() || value.get().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value.get().trim());
    }
}
