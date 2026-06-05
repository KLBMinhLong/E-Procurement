package com.eprocure.analytics.infrastructure.persistence.mapper;

import com.eprocure.analytics.infrastructure.persistence.entity.ReportDatasetRowDbEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReportDatasetMapper {
    @Select("""
            WITH po AS (
                SELECT po.po_id, po.pr_id, po.vendor_name, po.total_amount
                FROM analytics.po_issued_projections po
                WHERE po.is_deleted = FALSE
                  AND (#{fromInclusive,jdbcType=TIMESTAMP} IS NULL OR po.issued_at >= #{fromInclusive,jdbcType=TIMESTAMP})
                  AND (#{toExclusive,jdbcType=TIMESTAMP} IS NULL OR po.issued_at < #{toExclusive,jdbcType=TIMESTAMP})
                  AND (#{vendorId,jdbcType=OTHER} IS NULL OR po.vendor_id = #{vendorId,jdbcType=OTHER})
                  AND (#{categoryCode,jdbcType=VARCHAR} IS NULL OR EXISTS (
                      SELECT 1
                      FROM analytics.po_issued_line_projections filter_line
                      WHERE filter_line.po_id = po.po_id
                        AND filter_line.is_deleted = FALSE
                        AND filter_line.category_code = #{categoryCode,jdbcType=VARCHAR}
                  ))
            ),
            po_line AS (
                SELECT line.po_line_item_id
                FROM analytics.po_issued_line_projections line
                JOIN po ON po.po_id = line.po_id
                WHERE line.is_deleted = FALSE
                  AND (#{categoryCode,jdbcType=VARCHAR} IS NULL OR line.category_code = #{categoryCode,jdbcType=VARCHAR})
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
    List<ReportDatasetRowDbEntity> findPoSummaryRows(
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive,
            @Param("vendorId") UUID vendorId,
            @Param("categoryCode") String categoryCode);

    @Select("""
            WITH po AS (
                SELECT po.pr_id, po.pr_number, po.issued_at
                FROM analytics.po_issued_projections po
                WHERE po.is_deleted = FALSE
                  AND (#{fromInclusive,jdbcType=TIMESTAMP} IS NULL OR po.issued_at >= #{fromInclusive,jdbcType=TIMESTAMP})
                  AND (#{toExclusive,jdbcType=TIMESTAMP} IS NULL OR po.issued_at < #{toExclusive,jdbcType=TIMESTAMP})
                  AND (#{vendorId,jdbcType=OTHER} IS NULL OR po.vendor_id = #{vendorId,jdbcType=OTHER})
                  AND (#{categoryCode,jdbcType=VARCHAR} IS NULL OR EXISTS (
                      SELECT 1
                      FROM analytics.po_issued_line_projections filter_line
                      WHERE filter_line.po_id = po.po_id
                        AND filter_line.is_deleted = FALSE
                        AND filter_line.category_code = #{categoryCode,jdbcType=VARCHAR}
                  ))
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
    List<ReportDatasetRowDbEntity> findPrSummaryRows(
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive,
            @Param("vendorId") UUID vendorId,
            @Param("categoryCode") String categoryCode);

    @Select("""
            WITH breach AS (
                SELECT approver_role, assigned_at, breached_at
                FROM analytics.approval_sla_breach_projections
                WHERE is_deleted = FALSE
                  AND (#{fromInclusive,jdbcType=TIMESTAMP} IS NULL OR breached_at >= #{fromInclusive,jdbcType=TIMESTAMP})
                  AND (#{toExclusive,jdbcType=TIMESTAMP} IS NULL OR breached_at < #{toExclusive,jdbcType=TIMESTAMP})
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
    List<ReportDatasetRowDbEntity> findSlaComplianceRows(
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive);

    @Select("""
            WITH invoice AS (
                SELECT invoice_id, invoice_number, po_id, total_amount, matched_at
                FROM analytics.invoice_matched_projections
                WHERE is_deleted = FALSE
                  AND (#{fromInclusive,jdbcType=TIMESTAMP} IS NULL OR matched_at >= #{fromInclusive,jdbcType=TIMESTAMP})
                  AND (#{toExclusive,jdbcType=TIMESTAMP} IS NULL OR matched_at < #{toExclusive,jdbcType=TIMESTAMP})
                  AND (#{vendorId,jdbcType=OTHER} IS NULL OR vendor_id = #{vendorId,jdbcType=OTHER})
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
    List<ReportDatasetRowDbEntity> findThreeWayMatchRows(
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive,
            @Param("vendorId") UUID vendorId);
}
