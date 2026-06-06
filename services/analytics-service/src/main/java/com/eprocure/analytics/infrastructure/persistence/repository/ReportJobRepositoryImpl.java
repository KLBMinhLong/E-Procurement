package com.eprocure.analytics.infrastructure.persistence.repository;

import com.eprocure.analytics.domain.model.report.ReportFilterCriteria;
import com.eprocure.analytics.domain.model.report.ReportJob;
import com.eprocure.analytics.domain.repository.ReportJobRepository;
import com.eprocure.analytics.infrastructure.persistence.entity.ReportJobDbEntity;
import com.eprocure.analytics.infrastructure.persistence.mapper.ReportJobMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
                .map(this::toDomain);
    }

    @Override
    public Optional<ReportJob> findByIdAndActorId(UUID jobId, UUID actorId) {
        return mapper.findByIdAndActorId(jobId, actorId)
                .map(this::toDomain);
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

    @Override
    public List<ReportJob> claimQueuedForProcessing(int limit, Instant claimedAt) {
        return mapper.claimQueuedForProcessing(limit, claimedAt).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void markCompleted(UUID jobId, String downloadUrl, String storagePath, Instant completedAt, Instant expiresAt) {
        mapper.markCompleted(jobId, downloadUrl, storagePath, completedAt, expiresAt);
    }

    @Override
    public void markFailed(UUID jobId, String failureReason, Instant failedAt) {
        mapper.markFailed(jobId, failureReason, failedAt);
    }

    private String toJson(JsonNode filters) {
        try {
            return objectMapper.writeValueAsString(filters == null || filters.isNull() ? objectMapper.createObjectNode() : filters);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Cannot serialize report filters", exception);
        }
    }

    private ReportJob toDomain(ReportJobDbEntity row) {
        return row.toDomain(toFilterCriteria(row.getFiltersJson()));
    }

    private ReportFilterCriteria toFilterCriteria(String filtersJson) {
        if (filtersJson == null || filtersJson.isBlank()) {
            return ReportFilterCriteria.empty();
        }
        try {
            JsonNode root = objectMapper.readTree(filtersJson);
            return new ReportFilterCriteria(
                    localDate(root, "fromDate", "from_date"),
                    localDate(root, "toDate", "to_date"),
                    integer(root, "fiscalYear", "fiscal_year"),
                    integer(root, "quarter", "quarter"),
                    uuid(root, "vendorId", "vendor_id"),
                    text(root, "categoryCode", "category_code"));
        } catch (Exception exception) {
            throw new IllegalArgumentException("Cannot parse report filters", exception);
        }
    }

    private LocalDate localDate(JsonNode root, String camelName, String snakeName) {
        JsonNode value = field(root, camelName, snakeName);
        return value == null || !value.isTextual() || value.asText().isBlank()
                ? null
                : LocalDate.parse(value.asText());
    }

    private Integer integer(JsonNode root, String camelName, String snakeName) {
        JsonNode value = field(root, camelName, snakeName);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.isNumber() ? value.asInt() : Integer.valueOf(value.asText());
    }

    private UUID uuid(JsonNode root, String camelName, String snakeName) {
        JsonNode value = field(root, camelName, snakeName);
        return value == null || !value.isTextual() || value.asText().isBlank()
                ? null
                : UUID.fromString(value.asText());
    }

    private String text(JsonNode root, String camelName, String snakeName) {
        JsonNode value = field(root, camelName, snakeName);
        return value == null || !value.isTextual() || value.asText().isBlank()
                ? null
                : value.asText();
    }

    private JsonNode field(JsonNode root, String camelName, String snakeName) {
        if (root == null || !root.isObject()) {
            return null;
        }
        JsonNode camel = root.get(camelName);
        return camel == null || camel.isNull() ? root.get(snakeName) : camel;
    }
}
