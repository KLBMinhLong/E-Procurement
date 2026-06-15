package com.eprocure.admin.infrastructure.persistence.mapper;

import com.eprocure.admin.infrastructure.persistence.entity.AuditExportJobDbEntity;
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
public interface AuditExportJobMapper {

    @Select("""
            SELECT id,
                   status,
                   from_time,
                   to_time,
                   actor_id AS filter_actor_id,
                   entity_type,
                   action,
                   file_name,
                   storage_path,
                   failure_reason,
                   idempotency_key,
                   requested_at,
                   completed_at,
                   expires_at,
                   created_by
            FROM audit.audit_export_jobs
            WHERE created_by = #{createdBy,typeHandler=com.eprocure.admin.infrastructure.persistence.typehandler.UuidTypeHandler}
              AND idempotency_key = #{idempotencyKey,typeHandler=com.eprocure.admin.infrastructure.persistence.typehandler.UuidTypeHandler}
              AND is_deleted = FALSE
            LIMIT 1
            """)
    Optional<AuditExportJobDbEntity> findByIdempotencyKey(
            @Param("createdBy") UUID createdBy,
            @Param("idempotencyKey") UUID idempotencyKey);

    @Select("""
            SELECT id,
                   status,
                   from_time,
                   to_time,
                   actor_id AS filter_actor_id,
                   entity_type,
                   action,
                   file_name,
                   storage_path,
                   failure_reason,
                   idempotency_key,
                   requested_at,
                   completed_at,
                   expires_at,
                   created_by
            FROM audit.audit_export_jobs
            WHERE id = #{jobId,typeHandler=com.eprocure.admin.infrastructure.persistence.typehandler.UuidTypeHandler}
              AND created_by = #{actorId,typeHandler=com.eprocure.admin.infrastructure.persistence.typehandler.UuidTypeHandler}
              AND is_deleted = FALSE
            LIMIT 1
            """)
    Optional<AuditExportJobDbEntity> findByIdAndActorId(
            @Param("jobId") UUID jobId,
            @Param("actorId") UUID actorId);

    @Insert("""
            INSERT INTO audit.audit_export_jobs (
                id,
                status,
                from_time,
                to_time,
                actor_id,
                entity_type,
                action,
                idempotency_key,
                requested_at,
                completed_at,
                expires_at,
                created_by,
                created_at,
                updated_at
            )
            VALUES (
                #{entity.id,typeHandler=com.eprocure.admin.infrastructure.persistence.typehandler.UuidTypeHandler},
                #{entity.status},
                #{entity.fromTime},
                #{entity.toTime},
                #{entity.filterActorId,typeHandler=com.eprocure.admin.infrastructure.persistence.typehandler.UuidTypeHandler},
                #{entity.entityType},
                #{entity.action},
                #{entity.idempotencyKey,typeHandler=com.eprocure.admin.infrastructure.persistence.typehandler.UuidTypeHandler},
                #{entity.requestedAt},
                #{entity.completedAt},
                #{entity.expiresAt},
                #{entity.createdBy,typeHandler=com.eprocure.admin.infrastructure.persistence.typehandler.UuidTypeHandler},
                #{entity.requestedAt},
                #{entity.requestedAt}
            )
            """)
    void insertQueued(@Param("entity") AuditExportJobDbEntity entity);

    @Select("""
            WITH claimed AS (
                SELECT id
                FROM audit.audit_export_jobs
                WHERE status = 'QUEUED'
                  AND is_deleted = FALSE
                ORDER BY requested_at ASC
                LIMIT #{limit}
                FOR UPDATE SKIP LOCKED
            )
            UPDATE audit.audit_export_jobs job
            SET status = 'PROCESSING',
                updated_at = #{claimedAt}
            FROM claimed
            WHERE job.id = claimed.id
            RETURNING job.id,
                      job.status,
                      job.from_time,
                      job.to_time,
                      job.actor_id AS filter_actor_id,
                      job.entity_type,
                      job.action,
                      job.file_name,
                      job.storage_path,
                      job.failure_reason,
                      job.idempotency_key,
                      job.requested_at,
                      job.completed_at,
                      job.expires_at,
                      job.created_by
            """)
    List<AuditExportJobDbEntity> claimQueuedForProcessing(
            @Param("limit") int limit,
            @Param("claimedAt") Instant claimedAt);

    @Update("""
            UPDATE audit.audit_export_jobs
            SET status = 'COMPLETED',
                file_name = #{fileName},
                storage_path = #{storagePath},
                failure_reason = NULL,
                completed_at = #{completedAt},
                expires_at = #{expiresAt},
                updated_at = #{completedAt}
            WHERE id = #{jobId,typeHandler=com.eprocure.admin.infrastructure.persistence.typehandler.UuidTypeHandler}
              AND status = 'PROCESSING'
              AND is_deleted = FALSE
            """)
    int markCompleted(
            @Param("jobId") UUID jobId,
            @Param("fileName") String fileName,
            @Param("storagePath") String storagePath,
            @Param("completedAt") Instant completedAt,
            @Param("expiresAt") Instant expiresAt);

    @Update("""
            UPDATE audit.audit_export_jobs
            SET status = 'FAILED',
                failure_reason = #{failureReason},
                completed_at = #{failedAt},
                updated_at = #{failedAt}
            WHERE id = #{jobId,typeHandler=com.eprocure.admin.infrastructure.persistence.typehandler.UuidTypeHandler}
              AND status = 'PROCESSING'
              AND is_deleted = FALSE
            """)
    int markFailed(
            @Param("jobId") UUID jobId,
            @Param("failureReason") String failureReason,
            @Param("failedAt") Instant failedAt);
}
