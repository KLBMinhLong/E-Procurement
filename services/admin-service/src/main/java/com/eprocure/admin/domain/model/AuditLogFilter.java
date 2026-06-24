package com.eprocure.admin.domain.model;

import java.time.Instant;
import java.util.UUID;

public record AuditLogFilter(
        UUID actorId,
        String entityType,
        UUID entityId,
        String action,
        String serviceName,
        Boolean success,
        Instant fromTime,
        Instant toTime,
        int page,
        int size,
        int offset) {

    public AuditLogFilter {
        entityType = normalize(entityType);
        action = normalize(action);
        serviceName = normalize(serviceName);
        page = Math.max(page, 1);
        size = Math.max(1, Math.min(size, 200));
        offset = Math.max(offset, 0);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
