package com.eprocure.analytics.infrastructure.persistence.repository;

import com.eprocure.analytics.domain.model.report.ReportJob;
import com.eprocure.analytics.domain.repository.ReportJobRepository;
import com.eprocure.analytics.infrastructure.persistence.mapper.ReportJobMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class ReportJobRepositoryImpl implements ReportJobRepository {
    private final ReportJobMapper mapper;
    private final ObjectMapper objectMapper;

    public ReportJobRepositoryImpl(ReportJobMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<ReportJob> findByIdempotencyKey(UUID actorId, UUID idempotencyKey) {
        return mapper.findByIdempotencyKey(actorId, idempotencyKey)
                .map(row -> row.toDomain());
    }

    @Override
    public Optional<ReportJob> findByIdAndActorId(UUID jobId, UUID actorId) {
        return mapper.findByIdAndActorId(jobId, actorId)
                .map(row -> row.toDomain());
    }

    @Override
    public void saveQueued(ReportJob job, JsonNode filters) {
        mapper.insertQueued(
                job.id(),
                job.reportType().name(),
                job.format().name(),
                job.status().name(),
                toJson(filters),
                job.downloadUrl(),
                job.idempotencyKey(),
                job.createdAt(),
                job.completedAt(),
                job.expiresAt(),
                job.createdBy());
    }

    private String toJson(JsonNode filters) {
        try {
            return objectMapper.writeValueAsString(filters == null || filters.isNull() ? objectMapper.createObjectNode() : filters);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Cannot serialize report filters", exception);
        }
    }
}
