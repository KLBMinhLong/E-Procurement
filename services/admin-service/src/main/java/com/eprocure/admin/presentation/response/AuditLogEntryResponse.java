package com.eprocure.admin.presentation.response;

import java.time.Instant;
import java.util.UUID;

public record AuditLogEntryResponse(
        long id,
        AuditActorResponse actor,
        String action,
        String entityType,
        UUID entityId,
        String entityNumber,
        Instant occurredAt,
        String httpMethod,
        String endpoint,
        String requestId,
        boolean isSuccess,
        String errorCode,
        Object oldValue,
        Object newValue,
        String description,
        String serviceName) {
}
