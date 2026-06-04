package com.eprocure.analytics.infrastructure.persistence.mapper;

import com.eprocure.analytics.infrastructure.persistence.entity.ReportJobDbEntity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReportJobMapper {
    @Select("""
            SELECT id, report_type, format, status, download_url, created_at, completed_at,
                   expires_at, created_by, idempotency_key
            FROM analytics.report_export_jobs
            WHERE created_by = #{actorId}
              AND idempotency_key = #{idempotencyKey}
              AND is_deleted = FALSE
            LIMIT 1
            """)
    Optional<ReportJobDbEntity> findByIdempotencyKey(
            @Param("actorId") UUID actorId,
            @Param("idempotencyKey") UUID idempotencyKey);

    @Select("""
            SELECT id, report_type, format, status, download_url, created_at, completed_at,
                   expires_at, created_by, idempotency_key
            FROM analytics.report_export_jobs
            WHERE id = #{jobId}
              AND created_by = #{actorId}
              AND is_deleted = FALSE
            LIMIT 1
            """)
    Optional<ReportJobDbEntity> findByIdAndActorId(
            @Param("jobId") UUID jobId,
            @Param("actorId") UUID actorId);

    @Insert("""
            INSERT INTO analytics.report_export_jobs (
                id, report_type, format, status, filters, download_url, idempotency_key,
                created_at, completed_at, expires_at, created_by
            )
            VALUES (
                #{id}, #{reportType}, #{format}, #{status}, CAST(#{filtersJson} AS jsonb), #{downloadUrl},
                #{idempotencyKey}, #{createdAt}, #{completedAt}, #{expiresAt}, #{createdBy}
            )
            """)
    void insertQueued(
            @Param("id") UUID id,
            @Param("reportType") String reportType,
            @Param("format") String format,
            @Param("status") String status,
            @Param("filtersJson") String filtersJson,
            @Param("downloadUrl") String downloadUrl,
            @Param("idempotencyKey") UUID idempotencyKey,
            @Param("createdAt") Instant createdAt,
            @Param("completedAt") Instant completedAt,
            @Param("expiresAt") Instant expiresAt,
            @Param("createdBy") UUID createdBy);
}
