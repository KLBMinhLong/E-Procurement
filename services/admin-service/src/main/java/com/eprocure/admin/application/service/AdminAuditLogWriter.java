package com.eprocure.admin.application.service;

import com.eprocure.admin.application.port.out.AdminAuditLogWriterPort;
import com.eprocure.admin.domain.model.AdminConfigAction;
import com.eprocure.admin.domain.model.AdminConfigActionType;
import com.eprocure.admin.domain.model.AuditActor;
import com.eprocure.admin.domain.model.AuditExportJob;
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
    private static final String CATALOG_CATEGORY_ENTITY_TYPE = "CATALOG_CATEGORY";
    private static final String DEPARTMENT_ENTITY_TYPE = "IAM_DEPARTMENT";
    private static final String AUDIT_EXPORT_JOB_ENTITY_TYPE = "AUDIT_EXPORT_JOB";
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

    @Override
    public void recordCatalogCategoryMutation(String auditAction, CatalogCategoryAdminView category, AdminAuditContext context) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("code", category.code());
        payload.put("parentCode", category.parentCode());
        payload.put("requiresSpecialApproval", category.requiresSpecialApproval());
        payload.put("specialApproverRole", category.specialApproverRole());
        payload.put("requiresRfqAbove", category.requiresRfqAbove());
        payload.put("capex", category.isCapex());
        payload.put("itemCount", category.itemCount());
        payload.put("deleted", category.isDeleted());
        auditLogRepository.append(new AuditLogEntry(
                0L,
                actor(context),
                auditAction,
                CATALOG_CATEGORY_ENTITY_TYPE,
                Optional.empty(),
                Optional.of(category.code()),
                Instant.now(clock),
                context.httpMethod(),
                context.endpoint(),
                context.requestId(),
                true,
                Optional.empty(),
                Optional.empty(),
                Optional.of(toJson(payload)),
                Optional.of("Admin catalog category mutation recorded"),
                SERVICE_NAME));
    }

    @Override
    public void recordDepartmentMutation(String auditAction, DepartmentAdminView department, AdminAuditContext context) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("departmentId", department.id());
        payload.put("code", department.code());
        payload.put("parentId", department.parentId());
        payload.put("headUserId", department.headUserId());
        payload.put("memberCount", department.memberCount());
        payload.put("childCount", department.childCount());
        payload.put("deleted", department.deleted());
        auditLogRepository.append(new AuditLogEntry(
                0L,
                actor(context),
                auditAction,
                DEPARTMENT_ENTITY_TYPE,
                Optional.of(department.id()),
                Optional.of(department.code()),
                Instant.now(clock),
                context.httpMethod(),
                context.endpoint(),
                context.requestId(),
                true,
                Optional.empty(),
                Optional.empty(),
                Optional.of(toJson(payload)),
                Optional.of("Admin department mutation recorded"),
                SERVICE_NAME));
    }

    @Override
    public void recordAuditExportRequest(AuditExportJob job, AdminAuditContext context) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("jobId", job.id());
        payload.put("status", job.status().name());
        payload.put("fromTime", job.fromTime().toString());
        payload.put("toTime", job.toTime().toString());
        payload.put("filterActorId", job.filterActorId().orElse(null));
        payload.put("entityType", job.entityType().orElse(null));
        payload.put("action", job.action().orElse(null));
        payload.put("expiresAt", job.expiresAt().map(Instant::toString).orElse(null));
        auditLogRepository.append(new AuditLogEntry(
                0L,
                actor(context),
                "AUDIT_EXPORT.REQUESTED",
                AUDIT_EXPORT_JOB_ENTITY_TYPE,
                Optional.of(job.id()),
                Optional.of(job.id().toString()),
                job.requestedAt(),
                context.httpMethod(),
                context.endpoint(),
                context.requestId(),
                true,
                Optional.empty(),
                Optional.empty(),
                Optional.of(toJson(payload)),
                Optional.of("Admin audit export request recorded"),
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
            throw new IllegalStateException("Unable to serialize admin audit payload", exception);
        }
    }
}
