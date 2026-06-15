package com.eprocure.admin.infrastructure.persistence.repository;

import com.eprocure.admin.domain.model.AuditExportJob;
import com.eprocure.admin.domain.model.AuditExportJobStatus;
import com.eprocure.admin.domain.repository.AuditExportJobRepository;
import com.eprocure.admin.infrastructure.persistence.entity.AuditExportJobDbEntity;
import com.eprocure.admin.infrastructure.persistence.mapper.AuditExportJobMapper;
import java.util.Optional;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

@Repository
public class AuditExportJobRepositoryImpl implements AuditExportJobRepository {
    private static final Logger log = LogManager.getLogger(AuditExportJobRepositoryImpl.class);

    private final AuditExportJobMapper mapper;

    public AuditExportJobRepositoryImpl(AuditExportJobMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<AuditExportJob> findByIdempotencyKey(UUID createdBy, UUID idempotencyKey) {
        log.debug("[REPO] findByIdempotencyKey audit_export_jobs | createdBy={}", createdBy);
        return mapper.findByIdempotencyKey(createdBy, idempotencyKey).map(this::toDomain);
    }

    @Override
    public AuditExportJob saveQueued(AuditExportJob job) {
        log.debug("[REPO] insertQueued audit_export_jobs | id={}", job.id());
        mapper.insertQueued(toEntity(job));
        return job;
    }

    private AuditExportJobDbEntity toEntity(AuditExportJob job) {
        AuditExportJobDbEntity entity = new AuditExportJobDbEntity();
        entity.setId(job.id());
        entity.setStatus(job.status().name());
        entity.setFromTime(job.fromTime());
        entity.setToTime(job.toTime());
        entity.setFilterActorId(job.filterActorId().orElse(null));
        entity.setEntityType(job.entityType().orElse(null));
        entity.setAction(job.action().orElse(null));
        entity.setIdempotencyKey(job.idempotencyKey());
        entity.setRequestedAt(job.requestedAt());
        entity.setCompletedAt(job.completedAt().orElse(null));
        entity.setExpiresAt(job.expiresAt().orElse(null));
        entity.setCreatedBy(job.createdBy());
        return entity;
    }

    private AuditExportJob toDomain(AuditExportJobDbEntity entity) {
        return new AuditExportJob(
                entity.getId(),
                AuditExportJobStatus.valueOf(entity.getStatus()),
                entity.getFromTime(),
                entity.getToTime(),
                Optional.ofNullable(entity.getFilterActorId()),
                Optional.ofNullable(entity.getEntityType()),
                Optional.ofNullable(entity.getAction()),
                entity.getIdempotencyKey(),
                entity.getRequestedAt(),
                Optional.ofNullable(entity.getCompletedAt()),
                Optional.ofNullable(entity.getExpiresAt()),
                entity.getCreatedBy());
    }
}
