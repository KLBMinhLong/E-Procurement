package com.eprocure.admin.application.service;

import com.eprocure.admin.application.port.out.AdminAuditLogWriterPort;
import com.eprocure.admin.domain.model.AdminConfigAction;
import com.eprocure.admin.domain.model.AdminConfigActionType;
import com.eprocure.admin.domain.model.AuditActor;
import com.eprocure.admin.domain.model.AuditLogEntry;
import com.eprocure.admin.domain.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AdminAuditLogWriter implements AdminAuditLogWriterPort {
    private static final String CONFIG_ACTION_ENTITY_TYPE = "ADMIN_CONFIG_ACTION";
    private static final String SESSION_ENTITY_TYPE = "IAM_SESSION";
    private static final String SERVICE_NAME = "admin-service";

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public AdminAuditLogWriter(AuditLogRepository auditLogRepository, ObjectMapper objectMapper, Clock clock) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public void recordConfigAction(AdminConfigAction action, AdminAuditContext context) {
        auditLogRepository.append(new AuditLogEntry(
                0L,
                actor(context),
                auditAction(action.actionType()),
                CONFIG_ACTION_ENTITY_TYPE,
                Optional.of(action.id()),
                action.serviceName().or(() -> action.keyVersion()),
                action.requestedAt(),
                context.httpMethod(),
                context.endpoint(),
                context.requestId(),
                true,
                Optional.empty(),
                Optional.empty(),
                Optional.of(toJson(action)),
                Optional.of(description(action)),
                SERVICE_NAME));
    }

    @Override
    public void recordSessionInvalidation(UUID sessionId, AdminAuditContext context) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("actionType", "INVALIDATE_SESSION");
        payload.put("sessionId", sessionId);
        payload.put("invalidated", true);
        auditLogRepository.append(new AuditLogEntry(
                0L,
                actor(context),
                "SESSION.INVALIDATE_REQUESTED",
                SESSION_ENTITY_TYPE,
                Optional.of(sessionId),
                Optional.of(sessionId.toString()),
                Instant.now(clock),
                context.httpMethod(),
                context.endpoint(),
                context.requestId(),
                true,
                Optional.empty(),
                Optional.empty(),
                Optional.of(toJson(payload)),
                Optional.of("Admin session invalidation request recorded"),
                SERVICE_NAME));
    }

    private AuditActor actor(AdminAuditContext context) {
        return new AuditActor(context.actorId(), context.actorName(), context.actorRoles(), context.actorIp().orElse(null));
    }

    private String auditAction(AdminConfigActionType type) {
        return switch (type) {
            case UPDATE_CONFIG -> "CONFIG.UPDATE_REQUESTED";
            case RESTART_SERVICE -> "SERVICE.RESTART_REQUESTED";
            case ROTATE_ENCRYPTION_KEY -> "ENCRYPTION.KEY_ROTATE_REQUESTED";
        };
    }

    private String description(AdminConfigAction action) {
        return switch (action.actionType()) {
            case UPDATE_CONFIG -> "Admin config update request recorded";
            case RESTART_SERVICE -> "Admin service restart request recorded";
            case ROTATE_ENCRYPTION_KEY -> "Admin encryption key rotation request recorded";
        };
    }

    private String toJson(AdminConfigAction action) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("actionId", action.id());
        payload.put("actionType", action.actionType().name());
        payload.put("status", action.status().name());
        payload.put("serviceName", action.serviceName().orElse(null));
        payload.put("variableCount", action.variableCount());
        payload.put("requiresRestart", action.requiresRestart());
        payload.put("estimatedDowntimeSeconds", action.estimatedDowntimeSeconds().orElse(null));
        payload.put("keyVersion", action.keyVersion().orElse(null));
        payload.put("applied", false);
        return toJson(payload);
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize admin config audit payload", exception);
        }
    }
}
