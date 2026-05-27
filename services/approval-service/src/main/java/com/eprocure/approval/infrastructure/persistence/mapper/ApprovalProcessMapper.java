package com.eprocure.approval.infrastructure.persistence.mapper;

import com.eprocure.approval.infrastructure.persistence.entity.ApprovalProcessDbEntity;
import com.eprocure.approval.infrastructure.persistence.entity.ApprovalStepDbEntity;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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

    @Select("""
            SELECT p.*,
                   p.entity_snapshot::text AS entity_snapshot
            FROM approval.approval_processes p
            JOIN approval.approval_steps s ON s.process_id = p.id
            WHERE s.id = #{stepId}
              AND s.is_deleted = FALSE
              AND p.status = 'RUNNING'
              AND p.is_deleted = FALSE
            """)
    @Results(id = "approvalProcessResult", value = {
            @Result(property = "entityType", column = "entity_type"),
            @Result(property = "entityId", column = "entity_id"),
            @Result(property = "entityNumber", column = "entity_number"),
            @Result(property = "entityTitle", column = "entity_title"),
            @Result(property = "requesterId", column = "requester_id"),
            @Result(property = "requesterDepartmentId", column = "requester_department_id"),
            @Result(property = "totalAmount", column = "total_amount"),
            @Result(property = "camundaProcessInstanceId", column = "camunda_process_instance_id"),
            @Result(property = "currentStepIndex", column = "current_step_index"),
            @Result(property = "entitySnapshotJson", column = "entity_snapshot"),
            @Result(property = "startedAt", column = "started_at"),
            @Result(property = "completedAt", column = "completed_at"),
            @Result(property = "createdBy", column = "created_by")
    })
    ApprovalProcessDbEntity findRunningByStepId(@Param("stepId") UUID stepId);

    @Select("""
            SELECT p.*,
                   p.entity_snapshot::text AS entity_snapshot
            FROM approval.approval_processes p
            JOIN approval.approval_steps s ON s.process_id = p.id
            WHERE s.camunda_task_id = #{camundaTaskId}
              AND s.is_deleted = FALSE
              AND p.status = 'RUNNING'
              AND p.is_deleted = FALSE
            """)
    @Results(id = "approvalProcessResultByTask", value = {
            @Result(property = "entityType", column = "entity_type"),
            @Result(property = "entityId", column = "entity_id"),
            @Result(property = "entityNumber", column = "entity_number"),
            @Result(property = "entityTitle", column = "entity_title"),
            @Result(property = "requesterId", column = "requester_id"),
            @Result(property = "requesterDepartmentId", column = "requester_department_id"),
            @Result(property = "totalAmount", column = "total_amount"),
            @Result(property = "camundaProcessInstanceId", column = "camunda_process_instance_id"),
            @Result(property = "currentStepIndex", column = "current_step_index"),
            @Result(property = "entitySnapshotJson", column = "entity_snapshot"),
            @Result(property = "startedAt", column = "started_at"),
            @Result(property = "completedAt", column = "completed_at"),
            @Result(property = "createdBy", column = "created_by")
    })
    ApprovalProcessDbEntity findRunningByCamundaTaskId(@Param("camundaTaskId") String camundaTaskId);

    @Select("""
            SELECT *
            FROM approval.approval_steps
            WHERE process_id = #{processId}
              AND is_deleted = FALSE
            ORDER BY step_index ASC, assigned_at ASC
            """)
    @Results(id = "approvalStepResult", value = {
            @Result(property = "processId", column = "process_id"),
            @Result(property = "stepIndex", column = "step_index"),
            @Result(property = "stepType", column = "step_type"),
            @Result(property = "approverRole", column = "approver_role"),
            @Result(property = "approverId", column = "approver_id"),
            @Result(property = "delegateId", column = "delegate_id"),
            @Result(property = "slaDeadline", column = "sla_deadline"),
            @Result(property = "assignedAt", column = "assigned_at"),
            @Result(property = "actedAt", column = "acted_at"),
            @Result(property = "camundaTaskId", column = "camunda_task_id"),
            @Result(property = "createdBy", column = "created_by")
    })
    java.util.List<ApprovalStepDbEntity> findStepsByProcessId(@Param("processId") UUID processId);

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

    @Update("""
            UPDATE approval.approval_processes
            SET status = #{entity.status},
                current_step_index = #{entity.currentStepIndex},
                completed_at = #{entity.completedAt},
                updated_at = NOW()
            WHERE id = #{entity.id}
              AND is_deleted = FALSE
            """)
    void updateProcessRuntime(@Param("entity") ApprovalProcessDbEntity entity);

    @Update("""
            UPDATE approval.approval_steps
            SET approver_id = #{entity.approverId},
                delegate_id = #{entity.delegateId},
                status = #{entity.status},
                action = #{entity.action},
                comment = #{entity.comment},
                acted_at = #{entity.actedAt},
                camunda_task_id = #{entity.camundaTaskId},
                updated_at = NOW()
            WHERE id = #{entity.id}
              AND is_deleted = FALSE
            """)
    void updateStep(@Param("entity") ApprovalStepDbEntity entity);
}
