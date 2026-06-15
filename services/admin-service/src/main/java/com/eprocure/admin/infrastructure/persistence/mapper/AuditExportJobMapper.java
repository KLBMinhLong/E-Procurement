package com.eprocure.admin.infrastructure.persistence.mapper;

import com.eprocure.admin.infrastructure.persistence.entity.AuditExportJobDbEntity;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
}
