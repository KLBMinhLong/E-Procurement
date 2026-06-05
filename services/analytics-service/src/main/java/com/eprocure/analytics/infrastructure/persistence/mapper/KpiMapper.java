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

    /**
     * Counts total approval steps assigned in the period.
     * This is the denominator for SLA compliance: compliance% = (total - breached) / total * 100.
     */
    @Select("""
            SELECT COUNT(*)
            FROM analytics.approval_step_assigned_projections
            WHERE is_deleted = FALSE
              AND assigned_at >= #{fromInclusive}
              AND assigned_at < #{toExclusive}
            """)
    int countTotalAssignedSteps(
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive);

    /**
     * Counts total SLA-breached steps in the period.
     * This is the numerator for overdue metrics and subtracted from total for on-time count.
     */
    @Select("""
            SELECT COUNT(*)
            FROM analytics.approval_sla_breach_projections
            WHERE is_deleted = FALSE
              AND breached_at >= #{fromInclusive}
              AND breached_at < #{toExclusive}
            """)
    int countBreachedSteps(
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive);

    @Select("""
            WITH role_stats AS (
                SELECT
                    a.approver_role AS role,
                    COUNT(*)::INTEGER AS total_count,
                    COALESCE((
                        SELECT COUNT(*)
                        FROM analytics.approval_sla_breach_projections b
                        WHERE b.is_deleted = FALSE
                          AND b.approver_role = a.approver_role
                          AND b.breached_at >= #{fromInclusive}
                          AND b.breached_at < #{toExclusive}
                    ), 0)::INTEGER AS overdue_count
                FROM analytics.approval_step_assigned_projections a
                WHERE a.is_deleted = FALSE
                  AND a.assigned_at >= #{fromInclusive}
                  AND a.assigned_at < #{toExclusive}
                GROUP BY a.approver_role
            )
            SELECT
                role,
                CASE WHEN total_count > 0
                    THEN ROUND(((total_count - overdue_count)::NUMERIC / total_count) * 100, 2)
                    ELSE 0
                END AS compliance_pct,
                COALESCE((
                    SELECT ROUND(AVG(GREATEST(EXTRACT(EPOCH FROM (b.breached_at - b.assigned_at)) / 3600.0, 0))::NUMERIC, 2)
                    FROM analytics.approval_sla_breach_projections b
                    WHERE b.is_deleted = FALSE
                      AND b.approver_role = role_stats.role
                      AND b.breached_at >= #{fromInclusive}
                      AND b.breached_at < #{toExclusive}
                ), 0) AS avg_action_hours,
                overdue_count
            FROM role_stats
            ORDER BY overdue_count DESC, role ASC
            """)
    List<ApproverRoleSlaDbEntity> findByApproverRole(
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive);

    @Select("""
            WITH approver_stats AS (
                SELECT
                    a.approver_id,
                    CONCAT('approver:', SUBSTRING(a.approver_id::TEXT FROM 1 FOR 8)) AS approver_name,
                    COUNT(*)::INTEGER AS total_count,
                    COALESCE((
                        SELECT COUNT(*)
                        FROM analytics.approval_sla_breach_projections b
                        WHERE b.is_deleted = FALSE
                          AND b.breached_approver_id = a.approver_id
                          AND b.breached_at >= #{fromInclusive}
                          AND b.breached_at < #{toExclusive}
                    ), 0)::INTEGER AS overdue_count
                FROM analytics.approval_step_assigned_projections a
                WHERE a.is_deleted = FALSE
                  AND a.assigned_at >= #{fromInclusive}
                  AND a.assigned_at < #{toExclusive}
                GROUP BY a.approver_id
            )
            SELECT
                approver_name,
                overdue_count,
                CASE WHEN total_count > 0
                    THEN ROUND(((total_count - overdue_count)::NUMERIC / total_count) * 100, 2)
                    ELSE 0
                END AS compliance_pct
            FROM approver_stats
            WHERE overdue_count > 0
            ORDER BY overdue_count DESC, approver_name ASC
            LIMIT 5
            """)
    List<WorstApproverSlaDbEntity> findWorstApprovers(
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive);
}
