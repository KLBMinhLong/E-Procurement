package com.eprocure.analytics.infrastructure.persistence.mapper;

import com.eprocure.analytics.infrastructure.persistence.entity.CategorySpendDbEntity;
import com.eprocure.analytics.infrastructure.persistence.entity.DepartmentSpendDbEntity;
import com.eprocure.analytics.infrastructure.persistence.entity.ExecutiveDashboardSnapshotDbEntity;
import com.eprocure.analytics.infrastructure.persistence.entity.MonthlyTrendDbEntity;
import com.eprocure.analytics.infrastructure.persistence.entity.TopVendorDbEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ExecutiveDashboardMapper {
    @Select("""
            SELECT id, fiscal_year, quarter, currency, total_spent, approved_pr_count,
                   rfq_savings, approval_on_time_percent, approval_avg_cycle_hours,
                   approval_overdue_count, cached_at
            FROM analytics.executive_dashboard_snapshots
            WHERE fiscal_year = #{fiscalYear}
              AND ((#{quarter} IS NULL AND quarter IS NULL) OR quarter = #{quarter})
              AND cached_at >= #{cachedAfter}
              AND is_deleted = FALSE
            ORDER BY cached_at DESC
            LIMIT 1
            """)
    Optional<ExecutiveDashboardSnapshotDbEntity> findLatest(
            @Param("fiscalYear") int fiscalYear,
            @Param("quarter") Integer quarter,
            @Param("cachedAfter") Instant cachedAfter);

    @Select("""
            SELECT department_code, department_name, spent, budget, utilization, status
            FROM analytics.department_spend_snapshots
            WHERE dashboard_id = #{dashboardId}
              AND is_deleted = FALSE
            ORDER BY spent DESC, department_code ASC
            """)
    List<DepartmentSpendDbEntity> findDepartments(@Param("dashboardId") UUID dashboardId);

    @Select("""
            SELECT category_code, spent, budget
            FROM analytics.category_spend_snapshots
            WHERE dashboard_id = #{dashboardId}
              AND is_deleted = FALSE
            ORDER BY spent DESC, category_code ASC
            """)
    List<CategorySpendDbEntity> findCategories(@Param("dashboardId") UUID dashboardId);

    @Select("""
            SELECT month_label AS month, spent, budget, pr_count
            FROM analytics.monthly_spend_snapshots
            WHERE dashboard_id = #{dashboardId}
              AND is_deleted = FALSE
            ORDER BY month_label ASC
            """)
    List<MonthlyTrendDbEntity> findMonthlyTrend(@Param("dashboardId") UUID dashboardId);

    @Select("""
            SELECT vendor_name, total_spent, order_count, avg_score
            FROM analytics.top_vendor_snapshots
            WHERE dashboard_id = #{dashboardId}
              AND is_deleted = FALSE
            ORDER BY total_spent DESC, vendor_name ASC
            LIMIT 10
            """)
    List<TopVendorDbEntity> findTopVendors(@Param("dashboardId") UUID dashboardId);
}
