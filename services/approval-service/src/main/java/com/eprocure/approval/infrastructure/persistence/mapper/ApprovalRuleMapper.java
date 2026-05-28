package com.eprocure.approval.infrastructure.persistence.mapper;

import com.eprocure.approval.infrastructure.persistence.entity.ApprovalRuleDbEntity;
import com.eprocure.approval.infrastructure.persistence.entity.ApprovalRuleStepDbEntity;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Many;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ApprovalRuleMapper {

    @Select("""
        SELECT
            id,
            rule_name,
            priority,
            is_active AS active,
            rule_type,
            min_value,
            max_value,
            categories,
            department_ids,
            priorities,
            description,
            created_at,
            updated_at,
            created_by
        FROM approval.approval_rules
        WHERE is_active = true
          AND is_deleted = false
        ORDER BY priority DESC, rule_name ASC
        """)
    @Results(id = "approvalRuleResult", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "ruleName", column = "rule_name"),
            @Result(property = "priority", column = "priority"),
            @Result(property = "active", column = "active"),
            @Result(property = "ruleType", column = "rule_type"),
            @Result(property = "minValue", column = "min_value"),
            @Result(property = "maxValue", column = "max_value"),
            @Result(property = "categories", column = "categories"),
            @Result(property = "departmentIds", column = "department_ids"),
            @Result(property = "priorities", column = "priorities"),
            @Result(property = "description", column = "description"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "createdBy", column = "created_by"),
            @Result(property = "steps", column = "id", many = @Many(select = "findStepsByRuleId"))
    })
    List<ApprovalRuleDbEntity> findActiveRules();

    @Select("""
        SELECT
            id,
            rule_name,
            priority,
            is_active AS active,
            rule_type,
            min_value,
            max_value,
            categories,
            department_ids,
            priorities,
            description,
            created_at,
            updated_at,
            created_by
        FROM approval.approval_rules
        WHERE is_deleted = false
        ORDER BY priority DESC, rule_name ASC
        """)
    @Results(id = "approvalRuleListResult", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "ruleName", column = "rule_name"),
            @Result(property = "priority", column = "priority"),
            @Result(property = "active", column = "active"),
            @Result(property = "ruleType", column = "rule_type"),
            @Result(property = "minValue", column = "min_value"),
            @Result(property = "maxValue", column = "max_value"),
            @Result(property = "categories", column = "categories"),
            @Result(property = "departmentIds", column = "department_ids"),
            @Result(property = "priorities", column = "priorities"),
            @Result(property = "description", column = "description"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "createdBy", column = "created_by"),
            @Result(property = "steps", column = "id", many = @Many(select = "findStepsByRuleId"))
    })
    List<ApprovalRuleDbEntity> findAllRules();

    @Select("""
        SELECT
            id,
            rule_name,
            priority,
            is_active AS active,
            rule_type,
            min_value,
            max_value,
            categories,
            department_ids,
            priorities,
            description,
            created_at,
            updated_at,
            created_by
        FROM approval.approval_rules
        WHERE id = #{id}
          AND is_deleted = false
        """)
    @Results(id = "approvalRuleByIdResult", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "ruleName", column = "rule_name"),
            @Result(property = "priority", column = "priority"),
            @Result(property = "active", column = "active"),
            @Result(property = "ruleType", column = "rule_type"),
            @Result(property = "minValue", column = "min_value"),
            @Result(property = "maxValue", column = "max_value"),
            @Result(property = "categories", column = "categories"),
            @Result(property = "departmentIds", column = "department_ids"),
            @Result(property = "priorities", column = "priorities"),
            @Result(property = "description", column = "description"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "createdBy", column = "created_by"),
            @Result(property = "steps", column = "id", many = @Many(select = "findStepsByRuleId"))
    })
    ApprovalRuleDbEntity findById(@Param("id") UUID id);

    @Select("""
        SELECT EXISTS (
            SELECT 1
            FROM approval.approval_rules
            WHERE is_deleted = false
              AND rule_name = #{ruleName}
              AND (#{excludedId} IS NULL OR id != #{excludedId})
        )
        """)
    boolean existsByRuleName(@Param("ruleName") String ruleName, @Param("excludedId") UUID excludedId);

    @Select("""
        SELECT
            id,
            rule_id,
            step_index,
            approver_role,
            step_type,
            sla_hours,
            is_required AS required
        FROM approval.approval_rule_steps
        WHERE rule_id = #{ruleId}
          AND is_deleted = false
        ORDER BY step_index ASC, approver_role ASC
        """)
    @Results(id = "approvalRuleStepResult", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "ruleId", column = "rule_id"),
            @Result(property = "stepIndex", column = "step_index"),
            @Result(property = "approverRole", column = "approver_role"),
            @Result(property = "stepType", column = "step_type"),
            @Result(property = "slaHours", column = "sla_hours"),
            @Result(property = "required", column = "required")
    })
    List<ApprovalRuleStepDbEntity> findStepsByRuleId(@Param("ruleId") UUID ruleId);

    @Insert("""
        INSERT INTO approval.approval_rules (
            id,
            rule_name,
            priority,
            is_active,
            rule_type,
            min_value,
            max_value,
            categories,
            department_ids,
            priorities,
            description,
            created_by
        ) VALUES (
            #{entity.id},
            #{entity.ruleName},
            #{entity.priority},
            #{entity.active},
            #{entity.ruleType},
            #{entity.minValue},
            #{entity.maxValue},
            #{entity.categories,jdbcType=ARRAY},
            #{entity.departmentIds,jdbcType=ARRAY},
            #{entity.priorities,jdbcType=ARRAY},
            #{entity.description},
            #{actorId}
        )
        """)
    void insertRule(@Param("entity") ApprovalRuleDbEntity entity, @Param("actorId") UUID actorId);

    @Update("""
        UPDATE approval.approval_rules
        SET rule_name = #{entity.ruleName},
            priority = #{entity.priority},
            is_active = #{entity.active},
            rule_type = #{entity.ruleType},
            min_value = #{entity.minValue},
            max_value = #{entity.maxValue},
            categories = #{entity.categories,jdbcType=ARRAY},
            department_ids = #{entity.departmentIds,jdbcType=ARRAY},
            priorities = #{entity.priorities,jdbcType=ARRAY},
            description = #{entity.description}
        WHERE id = #{entity.id}
          AND is_deleted = false
        """)
    void updateRule(@Param("entity") ApprovalRuleDbEntity entity, @Param("actorId") UUID actorId);

    @Update("""
        UPDATE approval.approval_rules
        SET is_active = false
        WHERE id = #{id}
          AND is_deleted = false
        """)
    void deactivateRule(@Param("id") UUID id, @Param("actorId") UUID actorId);

    @Update("""
        UPDATE approval.approval_rule_steps
        SET is_deleted = true,
            deleted_at = NOW(),
            deleted_by = #{actorId}
        WHERE rule_id = #{ruleId}
          AND is_deleted = false
        """)
    void softDeleteStepsByRuleId(@Param("ruleId") UUID ruleId, @Param("actorId") UUID actorId);

    @Insert("""
        INSERT INTO approval.approval_rule_steps (
            id,
            rule_id,
            step_index,
            approver_role,
            step_type,
            sla_hours,
            is_required,
            created_by
        ) VALUES (
            #{entity.id},
            #{entity.ruleId},
            #{entity.stepIndex},
            #{entity.approverRole},
            #{entity.stepType},
            #{entity.slaHours},
            #{entity.required},
            #{actorId}
        )
        """)
    void insertStep(@Param("entity") ApprovalRuleStepDbEntity entity, @Param("actorId") UUID actorId);
}
