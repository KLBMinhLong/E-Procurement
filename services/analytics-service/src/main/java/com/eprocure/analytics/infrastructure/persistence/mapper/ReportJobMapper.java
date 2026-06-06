package com.eprocure.analytics.infrastructure.persistence.mapper;

import com.eprocure.analytics.infrastructure.persistence.entity.ReportJobDbEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ReportJobMapper {
    @Select("""
            SELECT id, report_type, format, status, download_url, storage_path, failure_reason,
                   filters::TEXT AS filters_json,
                   created_at, completed_at, expires_at, created_by, idempotency_key
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
            SELECT id, report_type, format, status, download_url, storage_path, failure_reason,
                   filters::TEXT AS filters_json,
                   created_at, completed_at, expires_at, created_by, idempotency_key
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

    @Select("""
            WITH claimed AS (
                SELECT id
                FROM analytics.report_export_jobs
                WHERE status = 'QUEUED'
                  AND is_deleted = FALSE
                ORDER BY created_at ASC
                LIMIT #{limit}
                FOR UPDATE SKIP LOCKED
            )
            UPDATE analytics.report_export_jobs job
            SET status = 'PROCESSING',
                updated_at = #{claimedAt}
            FROM claimed
            WHERE job.id = claimed.id
            RETURNING job.id, job.report_type, job.format, job.status, job.download_url,
                      job.storage_path, job.failure_reason, job.created_at, job.completed_at,
                      job.filters::TEXT AS filters_json, job.expires_at, job.created_by, job.idempotency_key
            """)
    List<ReportJobDbEntity> claimQueuedForProcessing(
            @Param("limit") int limit,
            @Param("claimedAt") Instant claimedAt);

    @Update("""
            UPDATE analytics.report_export_jobs
            SET status = 'COMPLETED',
                download_url = #{downloadUrl},
                storage_path = #{storagePath},
                failure_reason = NULL,
                completed_at = #{completedAt},
                expires_at = #{expiresAt},
                updated_at = #{completedAt}
            WHERE id = #{jobId}
              AND status = 'PROCESSING'
              AND is_deleted = FALSE
            """)
    int markCompleted(
            @Param("jobId") UUID jobId,
            @Param("downloadUrl") String downloadUrl,
            @Param("storagePath") String storagePath,
            @Param("completedAt") Instant completedAt,
            @Param("expiresAt") Instant expiresAt);

    @Update("""
            UPDATE analytics.report_export_jobs
            SET status = 'FAILED',
                failure_reason = #{failureReason},
                completed_at = #{failedAt},
                updated_at = #{failedAt}
            WHERE id = #{jobId}
              AND status = 'PROCESSING'
              AND is_deleted = FALSE
            """)
    int markFailed(
            @Param("jobId") UUID jobId,
            @Param("failureReason") String failureReason,
            @Param("failedAt") Instant failedAt);
}
