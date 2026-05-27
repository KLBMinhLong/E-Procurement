package com.eprocure.approval.infrastructure.persistence.mapper;

import com.eprocure.approval.infrastructure.persistence.entity.ApprovalProcessDbEntity;
import com.eprocure.approval.infrastructure.persistence.entity.ApprovalStepDbEntity;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ApprovalProcessMapper {
    @Select("""
            SELECT EXISTS (
                SELECT 1
                FROM approval.approval_processes
                WHERE entity_type = #{entityType}
                  AND entity_id = #{entityId}
                  AND status = 'RUNNING'
                  AND is_deleted = FALSE
            )
            """)
    boolean existsRunningByEntity(@Param("entityType") String entityType, @Param("entityId") UUID entityId);

    @Insert("""
            INSERT INTO approval.approval_processes (
                id,
                entity_type,
                entity_id,
                entity_number,
                entity_title,
                requester_id,
                requester_department_id,
                total_amount,
                currency,
                priority,
                camunda_process_instance_id,
                status,
                current_step_index,
                entity_snapshot,
                started_at,
                created_by
            ) VALUES (
                #{entity.id},
                #{entity.entityType},
                #{entity.entityId},
                #{entity.entityNumber},
                #{entity.entityTitle},
                #{entity.requesterId},
                #{entity.requesterDepartmentId},
                #{entity.totalAmount},
                #{entity.currency},
                #{entity.priority},
                #{entity.camundaProcessInstanceId},
                #{entity.status},
                #{entity.currentStepIndex},
                CAST(#{entity.entitySnapshotJson} AS jsonb),
                #{entity.startedAt},
                #{entity.createdBy}
            )
            """)
    void insertProcess(@Param("entity") ApprovalProcessDbEntity entity);

    @Insert("""
            INSERT INTO approval.approval_steps (
                id,
                process_id,
                step_index,
                step_type,
                approver_role,
                approver_id,
                delegate_id,
                status,
                sla_deadline,
                assigned_at,
                camunda_task_id,
                created_by
            ) VALUES (
                #{entity.id},
                #{entity.processId},
                #{entity.stepIndex},
                #{entity.stepType},
                #{entity.approverRole},
                #{entity.approverId},
                #{entity.delegateId},
                #{entity.status},
                #{entity.slaDeadline},
                #{entity.assignedAt},
                #{entity.camundaTaskId},
                #{entity.createdBy}
            )
            """)
    void insertStep(@Param("entity") ApprovalStepDbEntity entity);
}
