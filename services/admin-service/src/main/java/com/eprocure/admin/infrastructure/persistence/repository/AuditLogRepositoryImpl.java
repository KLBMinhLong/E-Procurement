package com.eprocure.admin.infrastructure.persistence.repository;

import com.eprocure.admin.application.service.PageMeta;
import com.eprocure.admin.application.service.PageResult;
import com.eprocure.admin.domain.model.AuditActor;
import com.eprocure.admin.domain.model.AuditLogEntry;
import com.eprocure.admin.domain.model.AuditLogFilter;
import com.eprocure.admin.domain.repository.AuditLogRepository;
import com.eprocure.admin.infrastructure.persistence.entity.AuditLogDbEntity;
import com.eprocure.admin.infrastructure.persistence.mapper.AuditLogMapper;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

@Repository
public class AuditLogRepositoryImpl implements AuditLogRepository {
    private static final Logger log = LogManager.getLogger(AuditLogRepositoryImpl.class);
    private static final String DEFAULT_SORT = "occurredAt,desc";

    private final AuditLogMapper mapper;

    public AuditLogRepositoryImpl(AuditLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public PageResult<AuditLogEntry> findByFilter(AuditLogFilter filter) {
        log.debug("[REPO] findByFilter audit_logs | from={} | to={} | page={} | size={}",
                filter.fromTime(),
                filter.toTime(),
                filter.page(),
                filter.size());
        List<AuditLogDbEntity> rows = mapper.findByFilter(filter);
        long total = rows.isEmpty() ? 0L : rows.get(0).getTotalCount();
        return new PageResult<>(
                rows.stream().map(this::toDomain).toList(),
                PageMeta.of(total, filter.page(), filter.size(), DEFAULT_SORT));
    }

    @Override
    public List<AuditLogEntry> findForExport(AuditLogFilter filter, int limit) {
        log.debug("[REPO] findForExport audit_logs | from={} | to={} | limit={}",
                filter.fromTime(),
                filter.toTime(),
                limit);
        return mapper.findForExport(filter, limit).stream()
                .map(this::toDomain)
                .toList();
    }

    private AuditLogEntry toDomain(AuditLogDbEntity entity) {
        return new AuditLogEntry(
                entity.getId(),
                new AuditActor(
                        entity.getActorId(),
                        entity.getActorName(),
                        parseRoles(entity.getActorRolesText()),
                        entity.getActorIp()),
                entity.getAction(),
                entity.getEntityType(),
                Optional.ofNullable(entity.getEntityId()),
                Optional.ofNullable(entity.getEntityNumber()),
                entity.getOccurredAt(),
                Optional.ofNullable(entity.getHttpMethod()),
                Optional.ofNullable(entity.getEndpoint()),
                Optional.ofNullable(entity.getRequestId()),
                entity.isSuccess(),
                Optional.ofNullable(entity.getErrorCode()),
                Optional.ofNullable(entity.getOldValueJson()),
                Optional.ofNullable(entity.getNewValueJson()),
                Optional.ofNullable(entity.getDescription()),
                entity.getServiceName());
    }

    private List<String> parseRoles(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }
}
