package com.eprocure.finance.infrastructure.persistence.mapper;

import com.eprocure.finance.domain.repository.InvoiceFilter;
import com.eprocure.finance.infrastructure.persistence.entity.InvoiceDbEntity;
import com.eprocure.finance.infrastructure.persistence.entity.InvoiceLineItemDbEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InvoiceMapper {
    Optional<InvoiceDbEntity> findHeaderById(@Param("invoiceId") UUID invoiceId);

    Optional<InvoiceDbEntity> findHeaderByIdempotencyKey(@Param("idempotencyKey") UUID idempotencyKey);

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
                id, invoice_id, line_number, description, quantity,
                unit_price, tax_rate, tax_amount, total_price, currency, created_by
            ) VALUES (
                #{entity.id}, #{entity.invoiceId}, #{entity.lineNumber}, #{entity.description},
                #{entity.quantity}, #{entity.unitPrice}, #{entity.taxRate}, #{entity.taxAmount},
                #{entity.totalPrice}, #{entity.currency}, #{createdBy}
            )
            """)
    void insertLineItem(@Param("entity") InvoiceLineItemDbEntity entity, @Param("createdBy") UUID createdBy);
}
