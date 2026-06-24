package com.eprocure.analytics.infrastructure.persistence.mapper;

import com.eprocure.analytics.infrastructure.persistence.entity.ReportJobDbEntity;
import com.eprocure.analytics.infrastructure.persistence.typehandler.UuidTypeHandler;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.ResultType;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ReportJobMapper {
    @Select("""
            SELECT id, report_type, format, status, download_url, storage_path, failure_reason,
                   filters::TEXT AS filters_json,
                   created_at, completed_at, expires_at, created_by, idempotency_key
            FROM analytics.report_export_jobs
            WHERE created_by = #{actorId,typeHandler=com.eprocure.analytics.infrastructure.persistence.typehandler.UuidTypeHandler}
              AND idempotency_key = #{idempotencyKey,typeHandler=com.eprocure.analytics.infrastructure.persistence.typehandler.UuidTypeHandler}
              AND is_deleted = FALSE
            LIMIT 1
            """)
    @ResultType(ReportJobDbEntity.class)
    @Results(id = "reportJobResult", value = {
            @Result(property = "id", column = "id", typeHandler = UuidTypeHandler.class),
            @Result(property = "reportType", column = "report_type"),
            @Result(property = "format", column = "format"),
            @Result(property = "status", column = "status"),
            @Result(property = "downloadUrl", column = "download_url"),
            @Result(property = "storagePath", column = "storage_path"),
            @Result(property = "failureReason", column = "failure_reason"),
            @Result(property = "filtersJson", column = "filters_json"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "completedAt", column = "completed_at"),
            @Result(property = "expiresAt", column = "expires_at"),
            @Result(property = "createdBy", column = "created_by", typeHandler = UuidTypeHandler.class),
            @Result(property = "idempotencyKey", column = "idempotency_key", typeHandler = UuidTypeHandler.class)
    })
    Optional<ReportJobDbEntity> findByIdempotencyKey(
            @Param("actorId") UUID actorId,
            @Param("idempotencyKey") UUID idempotencyKey);

    @Select("""
            SELECT id, report_type, format, status, download_url, storage_path, failure_reason,
                   filters::TEXT AS filters_json,
                   created_at, completed_at, expires_at, created_by, idempotency_key
            FROM analytics.report_export_jobs
            WHERE id = #{jobId,typeHandler=com.eprocure.analytics.infrastructure.persistence.typehandler.UuidTypeHandler}
              AND created_by = #{actorId,typeHandler=com.eprocure.analytics.infrastructure.persistence.typehandler.UuidTypeHandler}
              AND is_deleted = FALSE
            LIMIT 1
            """)
    @ResultMap("reportJobResult")
    Optional<ReportJobDbEntity> findByIdAndActorId(
            @Param("jobId") UUID jobId,
            @Param("actorId") UUID actorId);

    @Insert("""
            INSERT INTO analytics.report_export_jobs (
                id, report_type, format, status, filters, download_url, idempotency_key,
                created_at, completed_at, expires_at, created_by
            )
            VALUES (
                #{id,typeHandler=com.eprocure.analytics.infrastructure.persistence.typehandler.UuidTypeHandler},
                #{reportType}, #{format}, #{status}, CAST(#{filtersJson} AS jsonb), #{downloadUrl},
                #{idempotencyKey,typeHandler=com.eprocure.analytics.infrastructure.persistence.typehandler.UuidTypeHandler},
                #{createdAt}, #{completedAt}, #{expiresAt},
                #{createdBy,typeHandler=com.eprocure.analytics.infrastructure.persistence.typehandler.UuidTypeHandler}
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
            SELECT id, report_type, format, status, download_url, storage_path, failure_reason,
                   filters::TEXT AS filters_json,
                   created_at, completed_at, expires_at, created_by, idempotency_key
            FROM analytics.report_export_jobs
            WHERE status = 'QUEUED'
              AND is_deleted = FALSE
            ORDER BY created_at ASC
            LIMIT #{limit}
            FOR UPDATE SKIP LOCKED
            """)
    @ResultMap("reportJobResult")
    List<ReportJobDbEntity> selectQueuedForProcessing(@Param("limit") int limit);

    @Update({
            "<script>",
            "UPDATE analytics.report_export_jobs",
            "SET status = 'PROCESSING',",
            "    updated_at = #{claimedAt}",
            "WHERE id IN",
            "<foreach item='id' collection='ids' open='(' separator=',' close=')'>",
            "    #{id,typeHandler=com.eprocure.analytics.infrastructure.persistence.typehandler.UuidTypeHandler}",
            "</foreach>",
            "</script>"
    })
    int updateStatusToProcessing(@Param("ids") List<UUID> ids, @Param("claimedAt") Instant claimedAt);

    @Update("""
            UPDATE analytics.report_export_jobs
            SET status = 'COMPLETED',
                download_url = #{downloadUrl},
                storage_path = #{storagePath},
                failure_reason = NULL,
                completed_at = #{completedAt},
                expires_at = #{expiresAt},
                updated_at = #{completedAt}
            WHERE id = #{jobId,typeHandler=com.eprocure.analytics.infrastructure.persistence.typehandler.UuidTypeHandler}
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
            WHERE id = #{jobId,typeHandler=com.eprocure.analytics.infrastructure.persistence.typehandler.UuidTypeHandler}
              AND status = 'PROCESSING'
              AND is_deleted = FALSE
            """)
    int markFailed(
            @Param("jobId") UUID jobId,
            @Param("failureReason") String failureReason,
            @Param("failedAt") Instant failedAt);
}
