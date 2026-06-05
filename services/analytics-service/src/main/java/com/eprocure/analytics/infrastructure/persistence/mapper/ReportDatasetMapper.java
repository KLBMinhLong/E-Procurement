package com.eprocure.analytics.infrastructure.persistence.mapper;

import com.eprocure.analytics.infrastructure.persistence.entity.ReportDatasetRowDbEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReportDatasetMapper {
    @Select("""
            WITH po AS (
                SELECT *
                FROM analytics.po_issued_projections
                WHERE is_deleted = FALSE
            ),
            po_line AS (
                SELECT line.*
                FROM analytics.po_issued_line_projections line
                JOIN po ON po.po_id = line.po_id
                WHERE line.is_deleted = FALSE
            )
            SELECT 'Issued PO count' AS label, COUNT(*)::TEXT AS value FROM po
            UNION ALL
            SELECT 'Issued PO total', COALESCE(ROUND(SUM(total_amount), 4), 0)::TEXT FROM po
            UNION ALL
            SELECT 'Approved PR count', COUNT(DISTINCT pr_id)::TEXT FROM po WHERE pr_id IS NOT NULL
            UNION ALL
            SELECT 'Line item count', COUNT(*)::TEXT FROM po_line
            UNION ALL
            SELECT 'Top vendor', COALESCE((
                SELECT vendor_name || ' (' || COUNT(*)::TEXT || ')'
                FROM po
                GROUP BY vendor_name
                ORDER BY COUNT(*) DESC, vendor_name ASC
                LIMIT 1
            ), 'N/A')
            """)
    List<ReportDatasetRowDbEntity> findPoSummaryRows();

    @Select("""
            WITH po AS (
                SELECT *
                FROM analytics.po_issued_projections
                WHERE is_deleted = FALSE
            )
            SELECT 'PRs with issued PO' AS label, COUNT(DISTINCT pr_id)::TEXT AS value FROM po WHERE pr_id IS NOT NULL
            UNION ALL
            SELECT 'POs linked to PR', COUNT(*)::TEXT FROM po WHERE pr_id IS NOT NULL
            UNION ALL
            SELECT 'Latest PR number', COALESCE((
                SELECT pr_number
                FROM po
                WHERE pr_number IS NOT NULL
                ORDER BY issued_at DESC
                LIMIT 1
            ), 'N/A')
            """)
    List<ReportDatasetRowDbEntity> findPrSummaryRows();

    @Select("""
            WITH breach AS (
                SELECT *
                FROM analytics.approval_sla_breach_projections
                WHERE is_deleted = FALSE
            )
            SELECT 'SLA breach count' AS label, COUNT(*)::TEXT AS value FROM breach
            UNION ALL
            SELECT 'Average breached action hours',
                   COALESCE(ROUND(AVG(GREATEST(EXTRACT(EPOCH FROM (breached_at - assigned_at)) / 3600.0, 0))::NUMERIC, 2), 0)::TEXT
            FROM breach
            UNION ALL
            SELECT 'Top breached role', COALESCE((
                SELECT approver_role || ' (' || COUNT(*)::TEXT || ')'
                FROM breach
                GROUP BY approver_role
                ORDER BY COUNT(*) DESC, approver_role ASC
                LIMIT 1
            ), 'N/A')
            """)
    List<ReportDatasetRowDbEntity> findSlaComplianceRows();

    @Select("""
            WITH invoice AS (
                SELECT *
                FROM analytics.invoice_matched_projections
                WHERE is_deleted = FALSE
            )
            SELECT 'Matched invoice count' AS label, COUNT(*)::TEXT AS value FROM invoice
            UNION ALL
            SELECT 'Matched invoice total', COALESCE(ROUND(SUM(total_amount), 4), 0)::TEXT FROM invoice
            UNION ALL
            SELECT 'Latest matched invoice', COALESCE((
                SELECT invoice_number
                FROM invoice
                ORDER BY matched_at DESC
                LIMIT 1
            ), 'N/A')
            UNION ALL
            SELECT 'Distinct matched POs', COUNT(DISTINCT po_id)::TEXT FROM invoice
            """)
    List<ReportDatasetRowDbEntity> findThreeWayMatchRows();
}
