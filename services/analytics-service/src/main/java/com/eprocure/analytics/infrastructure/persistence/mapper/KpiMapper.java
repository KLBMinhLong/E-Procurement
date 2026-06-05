package com.eprocure.analytics.infrastructure.persistence.mapper;

import com.eprocure.analytics.infrastructure.persistence.entity.ApproverRoleSlaDbEntity;
import com.eprocure.analytics.infrastructure.persistence.entity.WorstApproverSlaDbEntity;
import java.time.Instant;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface KpiMapper {
    @Select("""
            SELECT
                approver_role AS role,
                0 AS compliance_pct,
                COALESCE(
                    ROUND(AVG(GREATEST(EXTRACT(EPOCH FROM (breached_at - assigned_at)) / 3600.0, 0))::NUMERIC, 2),
                    0
                ) AS avg_action_hours,
                COUNT(*)::INTEGER AS overdue_count
            FROM analytics.approval_sla_breach_projections
            WHERE is_deleted = FALSE
              AND breached_at >= #{fromInclusive}
              AND breached_at < #{toExclusive}
            GROUP BY approver_role
            ORDER BY COUNT(*) DESC, approver_role ASC
            """)
    List<ApproverRoleSlaDbEntity> findByApproverRole(
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive);

    @Select("""
            SELECT
                CONCAT('approver:', SUBSTRING(breached_approver_id::TEXT FROM 1 FOR 8)) AS approver_name,
                COUNT(*)::INTEGER AS overdue_count,
                0 AS compliance_pct
            FROM analytics.approval_sla_breach_projections
            WHERE is_deleted = FALSE
              AND breached_at >= #{fromInclusive}
              AND breached_at < #{toExclusive}
            GROUP BY breached_approver_id
            ORDER BY COUNT(*) DESC, breached_approver_id ASC
            LIMIT 5
            """)
    List<WorstApproverSlaDbEntity> findWorstApprovers(
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive);
}
