package com.eprocure.finance.infrastructure.persistence.mapper;

import com.eprocure.finance.domain.repository.InvoiceFilter;
import com.eprocure.finance.domain.model.InvoiceStatus;
import com.eprocure.finance.domain.model.MatchStatus;
import com.eprocure.finance.infrastructure.persistence.entity.InvoiceDbEntity;
import com.eprocure.finance.infrastructure.persistence.entity.InvoiceLineItemDbEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface InvoiceMapper {
    Optional<InvoiceDbEntity> findHeaderById(@Param("invoiceId") UUID invoiceId);

    Optional<InvoiceDbEntity> findHeaderByIdempotencyKey(@Param("idempotencyKey") UUID idempotencyKey);

    Optional<InvoiceDbEntity> findHeaderByIdAndMatchIdempotencyKey(
            @Param("invoiceId") UUID invoiceId,
            @Param("idempotencyKey") UUID idempotencyKey);

    Optional<InvoiceDbEntity> findHeaderByIdAndApprovalIdempotencyKey(
            @Param("invoiceId") UUID invoiceId,
            @Param("idempotencyKey") UUID idempotencyKey);

    Optional<InvoiceDbEntity> findHeaderByIdAndDisputeIdempotencyKey(
            @Param("invoiceId") UUID invoiceId,
            @Param("idempotencyKey") UUID idempotencyKey);

    Optional<InvoiceDbEntity> findHeaderByVendorIdAndInvoiceNumber(
            @Param("vendorId") UUID vendorId,
            @Param("invoiceNumber") String invoiceNumber);

    List<InvoiceDbEntity> findHeadersByFilter(@Param("filter") InvoiceFilter filter);

    long countByFilter(@Param("filter") InvoiceFilter filter);

    List<InvoiceLineItemDbEntity> findLineItemsByInvoiceId(@Param("invoiceId") UUID invoiceId);

    List<InvoiceLineItemDbEntity> findLineItemsByInvoiceIds(@Param("invoiceIds") List<UUID> invoiceIds);

    @Insert("""
            INSERT INTO finance.invoices (
                id, invoice_number, vendor_id, po_id, subtotal, tax_amount,
                total_amount, currency, invoice_date, due_date, status,
                po_match_status, gr_match_status, qty_variance, price_variance,
                matched_at, matched_by, approved_by, approved_at,
                idempotency_key, created_at, created_by
            ) VALUES (
                #{entity.id}, #{entity.invoiceNumber}, #{entity.vendorId}, #{entity.poId},
                #{entity.subtotal}, #{entity.taxAmount}, #{entity.totalAmount}, #{entity.currency},
                #{entity.invoiceDate}, #{entity.dueDate}, #{entity.status},
                #{entity.poMatchStatus}, #{entity.grMatchStatus}, #{entity.qtyVariance}, #{entity.priceVariance},
                #{entity.matchedAt}, #{entity.matchedBy}, #{entity.approvedBy}, #{entity.approvedAt},
                #{entity.idempotencyKey}, #{entity.createdAt}, #{entity.createdBy}
            )
            """)
    void insertHeader(@Param("entity") InvoiceDbEntity entity);

    @Insert("""
            INSERT INTO finance.invoice_line_items (
                id, invoice_id, line_number, po_line_item_id, description, quantity,
                unit_price, tax_rate, tax_amount, total_price, currency, created_by
            ) VALUES (
                #{entity.id}, #{entity.invoiceId}, #{entity.lineNumber}, #{entity.poLineItemId},
                #{entity.description}, #{entity.quantity}, #{entity.unitPrice}, #{entity.taxRate}, #{entity.taxAmount},
                #{entity.totalPrice}, #{entity.currency}, #{createdBy}
            )
            """)
    void insertLineItem(@Param("entity") InvoiceLineItemDbEntity entity, @Param("createdBy") UUID createdBy);

    @Update("""
            UPDATE finance.invoices
            SET status = #{status},
                po_match_status = #{poMatchStatus},
                gr_match_status = #{grMatchStatus},
                qty_variance = #{qtyVariance},
                price_variance = #{priceVariance},
                matched_at = #{matchedAt},
                matched_by = #{matchedBy},
                match_idempotency_key = #{idempotencyKey},
                updated_by = #{matchedBy}
            WHERE id = #{invoiceId}
              AND is_deleted = FALSE
            """)
    void updateMatchResult(
            @Param("invoiceId") UUID invoiceId,
            @Param("status") InvoiceStatus status,
            @Param("poMatchStatus") MatchStatus poMatchStatus,
            @Param("grMatchStatus") MatchStatus grMatchStatus,
            @Param("qtyVariance") java.math.BigDecimal qtyVariance,
            @Param("priceVariance") java.math.BigDecimal priceVariance,
            @Param("matchedAt") java.time.Instant matchedAt,
            @Param("matchedBy") UUID matchedBy,
            @Param("idempotencyKey") UUID idempotencyKey);

    @Update("""
            UPDATE finance.invoices
            SET status = 'APPROVED',
                approved_by = #{approvedBy},
                approved_at = #{approvedAt},
                approval_idempotency_key = #{idempotencyKey},
                updated_by = #{approvedBy}
            WHERE id = #{invoiceId}
              AND is_deleted = FALSE
            """)
    void markApproved(
            @Param("invoiceId") UUID invoiceId,
            @Param("approvedBy") UUID approvedBy,
            @Param("approvedAt") java.time.Instant approvedAt,
            @Param("idempotencyKey") UUID idempotencyKey);

    @Update("""
            UPDATE finance.invoices
            SET status = 'DISPUTED',
                dispute_reason = #{reason},
                disputed_by = #{disputedBy},
                disputed_at = #{disputedAt},
                dispute_idempotency_key = #{idempotencyKey},
                updated_by = #{disputedBy}
            WHERE id = #{invoiceId}
              AND is_deleted = FALSE
            """)
    void markDisputed(
            @Param("invoiceId") UUID invoiceId,
            @Param("reason") String reason,
            @Param("disputedBy") UUID disputedBy,
            @Param("disputedAt") java.time.Instant disputedAt,
            @Param("idempotencyKey") UUID idempotencyKey);

    @Update("""
            UPDATE finance.invoices
            SET status = 'PAID',
                paid_by = #{paidBy},
                paid_at = #{paidAt},
                updated_by = #{paidBy}
            WHERE id = #{invoiceId}
              AND is_deleted = FALSE
            """)
    void markPaid(
            @Param("invoiceId") UUID invoiceId,
            @Param("paidBy") UUID paidBy,
            @Param("paidAt") java.time.Instant paidAt);
}
