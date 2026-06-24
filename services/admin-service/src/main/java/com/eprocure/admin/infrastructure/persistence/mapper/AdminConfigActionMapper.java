package com.eprocure.admin.infrastructure.persistence.mapper;

import com.eprocure.admin.infrastructure.persistence.entity.AdminConfigActionDbEntity;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AdminConfigActionMapper {

    @Select("""
            SELECT id,
                   action_type,
                   status,
                   service_name,
                   change_reason,
                   variable_count,
                   requires_restart,
                   estimated_downtime_seconds,
                   key_version,
                   idempotency_key,
                   requested_at,
                   created_by
            FROM audit.admin_config_actions
            WHERE created_by = #{createdBy,typeHandler=com.eprocure.admin.infrastructure.persistence.typehandler.UuidTypeHandler}
              AND idempotency_key = #{idempotencyKey,typeHandler=com.eprocure.admin.infrastructure.persistence.typehandler.UuidTypeHandler}
              AND is_deleted = FALSE
            LIMIT 1
            """)
    Optional<AdminConfigActionDbEntity> findByIdempotencyKey(
            @Param("createdBy") UUID createdBy,
            @Param("idempotencyKey") UUID idempotencyKey);

    @Insert("""
            INSERT INTO audit.admin_config_actions (
                id,
                action_type,
                status,
                service_name,
                change_reason,
                variable_count,
                requires_restart,
                estimated_downtime_seconds,
                key_version,
                idempotency_key,
                requested_at,
                created_at,
                updated_at,
                created_by
            )
            VALUES (
                #{entity.id,typeHandler=com.eprocure.admin.infrastructure.persistence.typehandler.UuidTypeHandler},
                #{entity.actionType},
                #{entity.status},
                #{entity.serviceName},
                #{entity.changeReason},
                #{entity.variableCount},
                #{entity.requiresRestart},
                #{entity.estimatedDowntimeSeconds},
                #{entity.keyVersion},
                #{entity.idempotencyKey,typeHandler=com.eprocure.admin.infrastructure.persistence.typehandler.UuidTypeHandler},
                #{entity.requestedAt},
                #{entity.requestedAt},
                #{entity.requestedAt},
                #{entity.createdBy,typeHandler=com.eprocure.admin.infrastructure.persistence.typehandler.UuidTypeHandler}
            )
            """)
    void insert(@Param("entity") AdminConfigActionDbEntity entity);
}
