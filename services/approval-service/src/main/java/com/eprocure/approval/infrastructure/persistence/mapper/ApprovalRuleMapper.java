package com.eprocure.approval.infrastructure.persistence.mapper;

import com.eprocure.approval.infrastructure.persistence.entity.ApprovalRuleDbEntity;
import com.eprocure.approval.infrastructure.persistence.entity.ApprovalRuleStepDbEntity;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Many;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

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
}
