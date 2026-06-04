package com.eprocure.analytics.infrastructure.persistence.mapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AnalyticsProjectionMapper {
    @Select("""
            SELECT EXISTS (
                SELECT 1
                FROM analytics.event_processing_log
                WHERE event_id = #{eventId}
                  AND status = 'PROCESSED'
                  AND is_deleted = FALSE
            )
            """)
    boolean existsProcessedEvent(@Param("eventId") String eventId);

    @Insert("""
            INSERT INTO analytics.event_processing_log (
                event_id, topic, partition_id, offset_value, handler_name, event_timestamp, processed_at, status
            )
            VALUES (
                #{eventId}, #{topic}, #{partitionId}, #{offsetValue}, #{handlerName}, #{eventTimestamp}, NOW(), 'PROCESSED'
            )
            """)
    void insertProcessedEvent(
            @Param("eventId") String eventId,
            @Param("topic") String topic,
            @Param("partitionId") int partitionId,
            @Param("offsetValue") long offsetValue,
            @Param("handlerName") String handlerName,
            @Param("eventTimestamp") Instant eventTimestamp);

    @Insert("""
            INSERT INTO analytics.po_issued_projections (
                po_id, po_number, pr_id, pr_number, vendor_id, vendor_name,
                total_amount, currency, issued_at, source_event_id, event_timestamp
            )
            VALUES (
                #{poId}, #{poNumber}, #{prId}, #{prNumber}, #{vendorId}, #{vendorName},
                #{totalAmount}, #{currency}, #{issuedAt}, #{sourceEventId}, #{eventTimestamp}
            )
            ON CONFLICT (po_id) DO UPDATE SET
                po_number = EXCLUDED.po_number,
                pr_id = EXCLUDED.pr_id,
                pr_number = EXCLUDED.pr_number,
                vendor_id = EXCLUDED.vendor_id,
                vendor_name = EXCLUDED.vendor_name,
                total_amount = EXCLUDED.total_amount,
                currency = EXCLUDED.currency,
                issued_at = EXCLUDED.issued_at,
                source_event_id = EXCLUDED.source_event_id,
                event_timestamp = EXCLUDED.event_timestamp,
                is_deleted = FALSE,
                deleted_at = NULL,
                deleted_by = NULL
            """)
    void upsertPoIssued(
            @Param("poId") UUID poId,
            @Param("poNumber") String poNumber,
            @Param("prId") UUID prId,
            @Param("prNumber") String prNumber,
            @Param("vendorId") UUID vendorId,
            @Param("vendorName") String vendorName,
            @Param("totalAmount") BigDecimal totalAmount,
            @Param("currency") String currency,
            @Param("issuedAt") Instant issuedAt,
            @Param("sourceEventId") String sourceEventId,
            @Param("eventTimestamp") Instant eventTimestamp);

    @Insert("""
            INSERT INTO analytics.po_issued_line_projections (
                po_line_item_id, po_id, pr_line_item_id, item_name, category_code,
                quantity, unit, unit_price, total_price, currency
            )
            VALUES (
                #{poLineItemId}, #{poId}, #{prLineItemId}, #{itemName}, #{categoryCode},
                #{quantity}, #{unit}, #{unitPrice}, #{totalPrice}, #{currency}
            )
            ON CONFLICT (po_line_item_id) DO UPDATE SET
                po_id = EXCLUDED.po_id,
                pr_line_item_id = EXCLUDED.pr_line_item_id,
                item_name = EXCLUDED.item_name,
                category_code = EXCLUDED.category_code,
                quantity = EXCLUDED.quantity,
                unit = EXCLUDED.unit,
                unit_price = EXCLUDED.unit_price,
                total_price = EXCLUDED.total_price,
                currency = EXCLUDED.currency,
                is_deleted = FALSE,
                deleted_at = NULL,
                deleted_by = NULL
            """)
    void upsertPoIssuedLine(
            @Param("poLineItemId") UUID poLineItemId,
            @Param("poId") UUID poId,
            @Param("prLineItemId") UUID prLineItemId,
            @Param("itemName") String itemName,
            @Param("categoryCode") String categoryCode,
            @Param("quantity") BigDecimal quantity,
            @Param("unit") String unit,
            @Param("unitPrice") BigDecimal unitPrice,
            @Param("totalPrice") BigDecimal totalPrice,
            @Param("currency") String currency);

    @Insert("""
            INSERT INTO analytics.invoice_matched_projections (
                invoice_id, invoice_number, po_id, po_number, vendor_id, vendor_name,
                total_amount, currency, due_date, matched_at, source_event_id, event_timestamp
            )
            VALUES (
                #{invoiceId}, #{invoiceNumber}, #{poId}, #{poNumber}, #{vendorId}, #{vendorName},
                #{totalAmount}, #{currency}, #{dueDate}, #{matchedAt}, #{sourceEventId}, #{eventTimestamp}
            )
            ON CONFLICT (invoice_id) DO UPDATE SET
                invoice_number = EXCLUDED.invoice_number,
                po_id = EXCLUDED.po_id,
                po_number = EXCLUDED.po_number,
                vendor_id = EXCLUDED.vendor_id,
                vendor_name = EXCLUDED.vendor_name,
                total_amount = EXCLUDED.total_amount,
                currency = EXCLUDED.currency,
                due_date = EXCLUDED.due_date,
                matched_at = EXCLUDED.matched_at,
                source_event_id = EXCLUDED.source_event_id,
                event_timestamp = EXCLUDED.event_timestamp,
                is_deleted = FALSE,
                deleted_at = NULL,
                deleted_by = NULL
            """)
    void upsertInvoiceMatched(
            @Param("invoiceId") UUID invoiceId,
            @Param("invoiceNumber") String invoiceNumber,
            @Param("poId") UUID poId,
            @Param("poNumber") String poNumber,
            @Param("vendorId") UUID vendorId,
            @Param("vendorName") String vendorName,
            @Param("totalAmount") BigDecimal totalAmount,
            @Param("currency") String currency,
            @Param("dueDate") LocalDate dueDate,
            @Param("matchedAt") Instant matchedAt,
            @Param("sourceEventId") String sourceEventId,
            @Param("eventTimestamp") Instant eventTimestamp);

    @Insert("""
            INSERT INTO analytics.approval_sla_breach_projections (
                approval_step_id, process_id, purchase_request_id, pr_number, priority,
                step_index, step_type, approver_role, breached_approver_id, escalated_to_approver_id,
                reassigned, assigned_at, sla_deadline, breached_at, source_event_id, event_timestamp
            )
            VALUES (
                #{approvalStepId}, #{processId}, #{purchaseRequestId}, #{prNumber}, #{priority},
                #{stepIndex}, #{stepType}, #{approverRole}, #{breachedApproverId}, #{escalatedToApproverId},
                #{reassigned}, #{assignedAt}, #{slaDeadline}, #{breachedAt}, #{sourceEventId}, #{eventTimestamp}
            )
            ON CONFLICT (approval_step_id) DO UPDATE SET
                process_id = EXCLUDED.process_id,
                purchase_request_id = EXCLUDED.purchase_request_id,
                pr_number = EXCLUDED.pr_number,
                priority = EXCLUDED.priority,
                step_index = EXCLUDED.step_index,
                step_type = EXCLUDED.step_type,
                approver_role = EXCLUDED.approver_role,
                breached_approver_id = EXCLUDED.breached_approver_id,
                escalated_to_approver_id = EXCLUDED.escalated_to_approver_id,
                reassigned = EXCLUDED.reassigned,
                assigned_at = EXCLUDED.assigned_at,
                sla_deadline = EXCLUDED.sla_deadline,
                breached_at = EXCLUDED.breached_at,
                source_event_id = EXCLUDED.source_event_id,
                event_timestamp = EXCLUDED.event_timestamp,
                is_deleted = FALSE,
                deleted_at = NULL,
                deleted_by = NULL
            """)
    void upsertApprovalSlaBreach(
            @Param("approvalStepId") UUID approvalStepId,
            @Param("processId") UUID processId,
            @Param("purchaseRequestId") UUID purchaseRequestId,
            @Param("prNumber") String prNumber,
            @Param("priority") String priority,
            @Param("stepIndex") int stepIndex,
            @Param("stepType") String stepType,
            @Param("approverRole") String approverRole,
            @Param("breachedApproverId") UUID breachedApproverId,
            @Param("escalatedToApproverId") UUID escalatedToApproverId,
            @Param("reassigned") boolean reassigned,
            @Param("assignedAt") Instant assignedAt,
            @Param("slaDeadline") Instant slaDeadline,
            @Param("breachedAt") Instant breachedAt,
            @Param("sourceEventId") String sourceEventId,
            @Param("eventTimestamp") Instant eventTimestamp);

    @Insert("""
            WITH period_po AS (
                SELECT *
                FROM analytics.po_issued_projections
                WHERE is_deleted = FALSE
                  AND EXTRACT(YEAR FROM issued_at) = #{fiscalYear}
                  AND (#{quarter,jdbcType=INTEGER} IS NULL OR EXTRACT(QUARTER FROM issued_at) = #{quarter,jdbcType=INTEGER})
            ),
            period_sla AS (
                SELECT *
                FROM analytics.approval_sla_breach_projections
                WHERE is_deleted = FALSE
                  AND EXTRACT(YEAR FROM breached_at) = #{fiscalYear}
                  AND (#{quarter,jdbcType=INTEGER} IS NULL OR EXTRACT(QUARTER FROM breached_at) = #{quarter,jdbcType=INTEGER})
            )
            INSERT INTO analytics.executive_dashboard_snapshots (
                id, fiscal_year, quarter, currency, total_spent, approved_pr_count,
                rfq_savings, approval_on_time_percent, approval_avg_cycle_hours,
                approval_overdue_count, cached_at
            )
            SELECT
                #{dashboardId},
                #{fiscalYear},
                #{quarter,jdbcType=INTEGER},
                COALESCE((SELECT currency FROM period_po ORDER BY issued_at DESC LIMIT 1), 'VND'),
                COALESCE((SELECT SUM(total_amount) FROM period_po), 0),
                COALESCE((SELECT COUNT(DISTINCT pr_id)::INTEGER FROM period_po), 0),
                0,
                0,
                COALESCE((
                    SELECT ROUND(AVG(EXTRACT(EPOCH FROM (breached_at - assigned_at)) / 3600.0)::NUMERIC, 2)
                    FROM period_sla
                ), 0),
                COALESCE((SELECT COUNT(*)::INTEGER FROM period_sla), 0),
                #{cachedAt}
            """)
    void insertExecutiveDashboardSnapshot(
            @Param("dashboardId") UUID dashboardId,
            @Param("fiscalYear") int fiscalYear,
            @Param("quarter") Integer quarter,
            @Param("cachedAt") Instant cachedAt);

    @Insert("""
            INSERT INTO analytics.category_spend_snapshots (
                dashboard_id, category_code, spent, budget
            )
            SELECT
                #{dashboardId},
                line.category_code,
                SUM(line.total_price),
                NULL
            FROM analytics.po_issued_projections po
            JOIN analytics.po_issued_line_projections line ON line.po_id = po.po_id
            WHERE po.is_deleted = FALSE
              AND line.is_deleted = FALSE
              AND EXTRACT(YEAR FROM po.issued_at) = #{fiscalYear}
              AND (#{quarter,jdbcType=INTEGER} IS NULL OR EXTRACT(QUARTER FROM po.issued_at) = #{quarter,jdbcType=INTEGER})
            GROUP BY line.category_code
            ORDER BY SUM(line.total_price) DESC, line.category_code ASC
            LIMIT 12
            """)
    void insertCategorySpendSnapshots(
            @Param("dashboardId") UUID dashboardId,
            @Param("fiscalYear") int fiscalYear,
            @Param("quarter") Integer quarter);

    @Insert("""
            INSERT INTO analytics.monthly_spend_snapshots (
                dashboard_id, month_label, spent, budget, pr_count
            )
            SELECT
                #{dashboardId},
                TO_CHAR(po.issued_at AT TIME ZONE 'UTC', 'YYYY-MM') AS month_label,
                SUM(po.total_amount),
                0,
                COUNT(DISTINCT po.pr_id)::INTEGER
            FROM analytics.po_issued_projections po
            WHERE po.is_deleted = FALSE
              AND EXTRACT(YEAR FROM po.issued_at) = #{fiscalYear}
              AND (#{quarter,jdbcType=INTEGER} IS NULL OR EXTRACT(QUARTER FROM po.issued_at) = #{quarter,jdbcType=INTEGER})
            GROUP BY month_label
            ORDER BY month_label ASC
            """)
    void insertMonthlySpendSnapshots(
            @Param("dashboardId") UUID dashboardId,
            @Param("fiscalYear") int fiscalYear,
            @Param("quarter") Integer quarter);

    @Insert("""
            INSERT INTO analytics.top_vendor_snapshots (
                dashboard_id, vendor_name, total_spent, order_count, avg_score
            )
            SELECT
                #{dashboardId},
                po.vendor_name,
                SUM(po.total_amount),
                COUNT(*)::INTEGER,
                0
            FROM analytics.po_issued_projections po
            WHERE po.is_deleted = FALSE
              AND EXTRACT(YEAR FROM po.issued_at) = #{fiscalYear}
              AND (#{quarter,jdbcType=INTEGER} IS NULL OR EXTRACT(QUARTER FROM po.issued_at) = #{quarter,jdbcType=INTEGER})
            GROUP BY po.vendor_name
            ORDER BY SUM(po.total_amount) DESC, po.vendor_name ASC
            LIMIT 10
            """)
    void insertTopVendorSnapshots(
            @Param("dashboardId") UUID dashboardId,
            @Param("fiscalYear") int fiscalYear,
            @Param("quarter") Integer quarter);
}
