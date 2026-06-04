package com.eprocure.analytics.infrastructure.persistence.mapper;

import com.eprocure.analytics.infrastructure.persistence.entity.PurchasingDashboardMetricsDbEntity;
import com.eprocure.analytics.infrastructure.persistence.entity.PurchasingVendorPerformanceDbEntity;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PurchasingDashboardMapper {
    @Select("""
            SELECT
                COUNT(po.po_id)::INTEGER AS issued_po_count,
                COALESCE(SUM(po.total_amount), 0) AS issued_po_total,
                COALESCE((
                    SELECT COUNT(*)::INTEGER
                    FROM analytics.invoice_matched_projections invoice
                    WHERE invoice.is_deleted = FALSE
                ), 0) AS matched_invoice_count,
                COALESCE((
                    SELECT latest.currency
                    FROM analytics.po_issued_projections latest
                    WHERE latest.is_deleted = FALSE
                    ORDER BY latest.issued_at DESC
                    LIMIT 1
                ), 'VND') AS currency
            FROM analytics.po_issued_projections po
            WHERE po.is_deleted = FALSE
            """)
    Optional<PurchasingDashboardMetricsDbEntity> findMetrics();

    @Select("""
            SELECT
                po.vendor_name,
                COUNT(*)::INTEGER AS pending_orders
            FROM analytics.po_issued_projections po
            WHERE po.is_deleted = FALSE
            GROUP BY po.vendor_name
            ORDER BY COUNT(*) DESC, po.vendor_name ASC
            LIMIT 10
            """)
    List<PurchasingVendorPerformanceDbEntity> findVendorPerformance();
}
