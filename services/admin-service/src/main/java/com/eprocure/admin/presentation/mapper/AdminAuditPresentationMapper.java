package com.eprocure.admin.presentation.mapper;

import com.eprocure.admin.domain.model.AuditActor;
import com.eprocure.admin.domain.model.AuditLogEntry;
import com.eprocure.admin.domain.model.AuditLogFilter;
import com.eprocure.admin.presentation.response.AuditActorResponse;
import com.eprocure.admin.presentation.response.AuditLogEntryResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AdminAuditPresentationMapper {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 50;

    private final ObjectMapper objectMapper;

    public AdminAuditPresentationMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AuditLogFilter toFilter(
            UUID actorId,
            String entityType,
            UUID entityId,
            String action,
            String serviceName,
            Boolean success,
            Instant fromTime,
            Instant toTime,
            Integer page,
            Integer size) {
        int sanitizedPage = page == null ? DEFAULT_PAGE : Math.max(page, 1);
        int sanitizedSize = size == null ? DEFAULT_SIZE : Math.max(1, Math.min(size, 200));
        return new AuditLogFilter(
                actorId,
                entityType,
                entityId,
                action,
                serviceName,
                success,
                fromTime,
                toTime,
                sanitizedPage,
                sanitizedSize,
                (sanitizedPage - 1) * sanitizedSize);
    }

    public List<AuditLogEntryResponse> toResponseList(List<AuditLogEntry> entries) {
        return entries.stream().map(this::toResponse).toList();
    }

    private AuditLogEntryResponse toResponse(AuditLogEntry entry) {
        return new AuditLogEntryResponse(
                entry.id(),
                toResponse(entry.actor()),
                entry.action(),
                entry.entityType(),
                entry.entityId().orElse(null),
                entry.entityNumber().orElse(null),
                entry.occurredAt(),
                entry.httpMethod().orElse(null),
                entry.endpoint().orElse(null),
                entry.requestId().orElse(null),
                entry.success(),
                entry.errorCode().orElse(null),
                parseJson(entry.oldValueJson().orElse(null)),
                parseJson(entry.newValueJson().orElse(null)),
                entry.description().orElse(null),
                entry.serviceName());
    }

    private AuditActorResponse toResponse(AuditActor actor) {
        return new AuditActorResponse(
                actor.id(),
                actor.name(),
                actor.roles(),
                actor.ip());
    }

    private Object parseJson(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(value);
        } catch (Exception exception) {
            return value;
        }
    }
}
