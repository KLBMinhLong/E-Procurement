package com.eprocure.analytics.infrastructure.persistence.mapper;

import com.eprocure.analytics.infrastructure.persistence.entity.ManagerDashboardMetricsDbEntity;
import com.eprocure.analytics.infrastructure.persistence.entity.ManagerSlaWarningDbEntity;
import com.eprocure.analytics.infrastructure.persistence.entity.RecentPurchaseRequestDbEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ManagerDashboardMapper {
    @Select("""
            SELECT
                COUNT(pr.pr_id)::INTEGER AS submitted_pr_count,
                COALESCE(SUM(pr.total_amount), 0) AS submitted_pr_total,
                COALESCE((
                    SELECT COUNT(*)::INTEGER
                    FROM analytics.approval_sla_breach_projections sla
                    WHERE sla.is_deleted = FALSE
                ), 0) AS sla_breach_count,
                COALESCE((
                    SELECT latest.currency
                    FROM analytics.pr_submitted_projections latest
                    WHERE latest.is_deleted = FALSE
                    ORDER BY latest.submitted_at DESC
                    LIMIT 1
                ), 'VND') AS currency
            FROM analytics.pr_submitted_projections pr
            WHERE pr.is_deleted = FALSE
              AND (#{departmentId}::UUID IS NULL OR pr.department_id = #{departmentId})
            """)
    Optional<ManagerDashboardMetricsDbEntity> findMetrics(@Param("departmentId") UUID departmentId);

    @Select("""
            SELECT
                sla.approval_step_id,
                sla.pr_number,
                sla.sla_deadline,
                CASE WHEN sla.breached_at <= NOW() THEN TRUE ELSE FALSE END AS overdue
            FROM analytics.approval_sla_breach_projections sla
            WHERE sla.is_deleted = FALSE
            ORDER BY sla.sla_deadline ASC
            LIMIT 10
            """)
    List<ManagerSlaWarningDbEntity> findSlaWarnings();

    @Select("""
            SELECT
                pr.pr_number,
                pr.priority,
                pr.total_amount,
                pr.currency,
                pr.submitted_at
            FROM analytics.pr_submitted_projections pr
            WHERE pr.is_deleted = FALSE
              AND (#{departmentId}::UUID IS NULL OR pr.department_id = #{departmentId})
            ORDER BY pr.submitted_at DESC
            LIMIT 10
            """)
    List<RecentPurchaseRequestDbEntity> findRecentPrs(@Param("departmentId") UUID departmentId);
}
