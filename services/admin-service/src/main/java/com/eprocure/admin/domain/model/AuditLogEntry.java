package com.eprocure.admin.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record AuditLogEntry(
        long id,
        AuditActor actor,
        String action,
        String entityType,
        Optional<UUID> entityId,
        Optional<String> entityNumber,
        Instant occurredAt,
        Optional<String> httpMethod,
        Optional<String> endpoint,
        Optional<String> requestId,
        boolean success,
        Optional<String> errorCode,
        Optional<String> oldValueJson,
        Optional<String> newValueJson,
        Optional<String> description,
        String serviceName) {

    public AuditLogEntry {
        actor = Objects.requireNonNull(actor, "actor must not be null");
        action = Objects.requireNonNull(action, "action must not be null").trim();
        entityType = Objects.requireNonNull(entityType, "entityType must not be null").trim();
        entityId = entityId == null ? Optional.empty() : entityId;
        entityNumber = normalize(entityNumber);
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        httpMethod = normalize(httpMethod);
        endpoint = normalize(endpoint);
        requestId = normalize(requestId);
        errorCode = normalize(errorCode);
        oldValueJson = normalize(oldValueJson);
        newValueJson = normalize(newValueJson);
        description = normalize(description);
        serviceName = Objects.requireNonNull(serviceName, "serviceName must not be null").trim();
    }

    private static Optional<String> normalize(Optional<String> value) {
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }
        String trimmed = value.get().trim();
        return trimmed.isBlank() ? Optional.empty() : Optional.of(trimmed);
    }
}
