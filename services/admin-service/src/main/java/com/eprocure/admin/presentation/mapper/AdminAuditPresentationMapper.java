package com.eprocure.admin.presentation.mapper;

import com.eprocure.admin.application.port.in.ExportAuditLogCommand;
import com.eprocure.admin.application.port.in.GetAuditExportJobQuery;
import com.eprocure.admin.application.service.AdminAuditContext;
import com.eprocure.admin.domain.model.AuditExportJob;
import com.eprocure.admin.domain.model.AuditActor;
import com.eprocure.admin.domain.model.AuditExportJobStatus;
import com.eprocure.admin.domain.model.AuditLogEntry;
import com.eprocure.admin.domain.model.AuditLogFilter;
import com.eprocure.admin.common.security.UserPrincipal;
import com.eprocure.admin.presentation.response.AuditActorResponse;
import com.eprocure.admin.presentation.request.AuditLogExportRequest;
import com.eprocure.admin.presentation.response.AuditExportJobResponse;
import com.eprocure.admin.presentation.response.AuditLogEntryResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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

    public ExportAuditLogCommand toCommand(
            UserPrincipal principal,
            AuditLogExportRequest request,
            AdminAuditContext auditContext) {
        return new ExportAuditLogCommand(
                principal.getId(),
                request.fromTime(),
                request.toTime(),
                Optional.ofNullable(request.actorId()),
                Optional.ofNullable(request.entityType()),
                Optional.ofNullable(request.action()),
                auditContext);
    }

    public AdminAuditContext toAuditContext(UserPrincipal principal, HttpServletRequest request, String requestId) {
        return new AdminAuditContext(
                principal.getId(),
                principal.getFullName(),
                principal.getPermissions().stream().sorted().toList(),
                Optional.ofNullable(request.getRemoteAddr()),
                Optional.ofNullable(request.getMethod()),
                Optional.ofNullable(request.getRequestURI()),
                Optional.ofNullable(requestId));
    }

    public GetAuditExportJobQuery toQuery(UserPrincipal principal, UUID jobId) {
        return new GetAuditExportJobQuery(principal.getId(), jobId);
    }

    public AuditExportJobResponse toResponse(AuditExportJob job) {
        return new AuditExportJobResponse(
                job.id(),
                job.status(),
                job.fromTime(),
                job.toTime(),
                job.filterActorId().orElse(null),
                job.entityType().orElse(null),
                job.action().orElse(null),
                job.fileName().orElse(null),
                downloadUrl(job),
                job.failureReason().orElse(null),
                job.requestedAt(),
                job.completedAt().orElse(null),
                job.expiresAt().orElse(null));
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

    private String downloadUrl(AuditExportJob job) {
        return job.status() == AuditExportJobStatus.COMPLETED && job.storagePath().isPresent()
                ? "/api/v1/admin/audit-log/export/" + job.id() + "/download"
                : null;
    }
}
