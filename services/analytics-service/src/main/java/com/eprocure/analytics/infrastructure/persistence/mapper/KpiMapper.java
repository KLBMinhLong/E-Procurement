package com.eprocure.analytics.infrastructure.persistence.mapper;

import com.eprocure.analytics.infrastructure.persistence.entity.ApproverRoleSlaDbEntity;
import com.eprocure.analytics.infrastructure.persistence.entity.CycleTimeSummaryDbEntity;
import com.eprocure.analytics.infrastructure.persistence.entity.PriorityCycleTimeDbEntity;
import com.eprocure.analytics.infrastructure.persistence.entity.WeeklyCycleTimeDbEntity;
import com.eprocure.analytics.infrastructure.persistence.entity.WorstApproverSlaDbEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface KpiMapper {
    @Select("""
            WITH cycle AS (
                SELECT
                    pr.pr_id,
                    GREATEST(EXTRACT(EPOCH FROM (MIN(po.issued_at) - pr.submitted_at)) / 3600.0, 0) AS cycle_hours
                FROM analytics.pr_submitted_projections pr
                JOIN analytics.po_issued_projections po ON po.pr_id = pr.pr_id
                WHERE pr.is_deleted = FALSE
                  AND po.is_deleted = FALSE
                  AND po.issued_at >= pr.submitted_at
                  AND pr.submitted_at >= #{fromInclusive}
                  AND pr.submitted_at < #{toExclusive}
                  AND (#{departmentId,jdbcType=OTHER} IS NULL OR pr.department_id = #{departmentId,jdbcType=OTHER})
                GROUP BY pr.pr_id, pr.submitted_at
            )
            SELECT
                COALESCE(ROUND(AVG(cycle_hours)::NUMERIC, 2), 0) AS avg_cycle_hours,
                COALESCE(ROUND((PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY cycle_hours))::NUMERIC, 2), 0) AS median_cycle_hours,
                COALESCE(ROUND((PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY cycle_hours))::NUMERIC, 2), 0) AS p95_cycle_hours
            FROM cycle
            """)
    CycleTimeSummaryDbEntity findCycleTimeSummary(
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive,
            @Param("departmentId") UUID departmentId);

    @Select("""
            WITH cycle AS (
                SELECT
                    pr.pr_id,
                    pr.priority,
                    GREATEST(EXTRACT(EPOCH FROM (MIN(po.issued_at) - pr.submitted_at)) / 3600.0, 0) AS cycle_hours
                FROM analytics.pr_submitted_projections pr
                JOIN analytics.po_issued_projections po ON po.pr_id = pr.pr_id
                WHERE pr.is_deleted = FALSE
                  AND po.is_deleted = FALSE
                  AND po.issued_at >= pr.submitted_at
                  AND pr.submitted_at >= #{fromInclusive}
                  AND pr.submitted_at < #{toExclusive}
                  AND (#{departmentId,jdbcType=OTHER} IS NULL OR pr.department_id = #{departmentId,jdbcType=OTHER})
                GROUP BY pr.pr_id, pr.priority, pr.submitted_at
            )
            SELECT priority, ROUND(AVG(cycle_hours)::NUMERIC, 2) AS avg_hours
            FROM cycle
            GROUP BY priority
            ORDER BY priority ASC
            """)
    List<PriorityCycleTimeDbEntity> findCycleTimeByPriority(
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive,
            @Param("departmentId") UUID departmentId);

    @Select("""
            WITH cycle AS (
                SELECT
                    pr.pr_id,
                    pr.submitted_at,
                    GREATEST(EXTRACT(EPOCH FROM (MIN(po.issued_at) - pr.submitted_at)) / 3600.0, 0) AS cycle_hours
                FROM analytics.pr_submitted_projections pr
                JOIN analytics.po_issued_projections po ON po.pr_id = pr.pr_id
                WHERE pr.is_deleted = FALSE
                  AND po.is_deleted = FALSE
                  AND po.issued_at >= pr.submitted_at
                  AND pr.submitted_at >= #{fromInclusive}
                  AND pr.submitted_at < #{toExclusive}
                  AND (#{departmentId,jdbcType=OTHER} IS NULL OR pr.department_id = #{departmentId,jdbcType=OTHER})
                GROUP BY pr.pr_id, pr.submitted_at
            )
            SELECT
                TO_CHAR(DATE_TRUNC('week', submitted_at AT TIME ZONE 'UTC'), 'IYYY-"W"IW') AS week,
                ROUND(AVG(cycle_hours)::NUMERIC, 2) AS avg_hours
            FROM cycle
            GROUP BY DATE_TRUNC('week', submitted_at AT TIME ZONE 'UTC')
            ORDER BY DATE_TRUNC('week', submitted_at AT TIME ZONE 'UTC') ASC
            """)
    List<WeeklyCycleTimeDbEntity> findCycleTimeTrend(
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive,
            @Param("departmentId") UUID departmentId);

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
