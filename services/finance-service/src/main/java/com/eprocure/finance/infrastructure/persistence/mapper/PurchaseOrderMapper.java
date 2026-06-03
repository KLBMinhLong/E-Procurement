package com.eprocure.finance.infrastructure.persistence.mapper;

import com.eprocure.finance.domain.repository.PurchaseOrderFilter;
import com.eprocure.finance.infrastructure.persistence.entity.PurchaseOrderDbEntity;
import com.eprocure.finance.infrastructure.persistence.entity.PurchaseOrderLineItemDbEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PurchaseOrderMapper {
    Optional<PurchaseOrderDbEntity> findHeaderById(@Param("poId") UUID poId);

    Optional<PurchaseOrderDbEntity> findHeaderBySourceEventId(@Param("sourceEventId") String sourceEventId);

    Optional<PurchaseOrderDbEntity> findHeaderByRfqId(@Param("rfqId") UUID rfqId);

    List<PurchaseOrderDbEntity> findHeadersByFilter(@Param("filter") PurchaseOrderFilter filter);

    long countByFilter(@Param("filter") PurchaseOrderFilter filter);

    List<PurchaseOrderLineItemDbEntity> findLineItemsByPoId(@Param("poId") UUID poId);

    List<PurchaseOrderLineItemDbEntity> findLineItemsByPoIds(@Param("poIds") List<UUID> poIds);

    @Select("""
            SELECT 'PO-' || CAST(#{fiscalYear} AS text) || '-' || LPAD(nextval('finance.po_number_seq')::text, 6, '0')
            """)
    String nextPoNumber(@Param("fiscalYear") int fiscalYear);

    @Insert("""
            INSERT INTO finance.purchase_orders (
                id, po_number, pr_id, pr_number, rfq_id, rfq_number, awarded_quote_id,
                vendor_id, vendor_name, vendor_email, vendor_tax_code,
                purchasing_officer_id, purchasing_officer_full_name, status,
                total_amount, currency, delivery_address, delivery_deadline, payment_terms,
                vendor_note, issued_at, sent_to_vendor_at, cancelled_at, cancelled_by, cancel_reason,
                created_at, created_by, source_event_id
            ) VALUES (
                #{entity.id}, #{entity.poNumber}, #{entity.prId}, #{entity.prNumber},
                #{entity.rfqId}, #{entity.rfqNumber}, #{entity.awardedQuoteId},
                #{entity.vendorId}, #{entity.vendorName}, #{entity.vendorEmail}, #{entity.vendorTaxCode},
                #{entity.purchasingOfficerId}, #{entity.purchasingOfficerFullName}, #{entity.status},
                #{entity.totalAmount}, #{entity.currency}, #{entity.deliveryAddress}, #{entity.deliveryDeadline},
                #{entity.paymentTerms}, #{entity.vendorNote}, #{entity.issuedAt}, #{entity.sentToVendorAt},
                #{entity.cancelledAt}, #{entity.cancelledBy}, #{entity.cancelReason}, #{entity.createdAt},
                #{entity.purchasingOfficerId}, #{entity.sourceEventId}
            )
            """)
    void insertPurchaseOrder(@Param("entity") PurchaseOrderDbEntity entity);

    @Update("""
            UPDATE finance.purchase_orders
            SET delivery_address = #{entity.deliveryAddress},
                delivery_deadline = #{entity.deliveryDeadline},
                payment_terms = #{entity.paymentTerms},
                updated_by = #{actorId}
            WHERE id = #{entity.id}
              AND is_deleted = FALSE
            """)
    int updateDraftDetails(@Param("entity") PurchaseOrderDbEntity entity, @Param("actorId") UUID actorId);

    @Update("""
            UPDATE finance.purchase_orders
            SET status = #{entity.status},
                vendor_note = #{entity.vendorNote},
                issued_at = #{entity.issuedAt},
                sent_to_vendor_at = #{entity.sentToVendorAt},
                cancelled_at = #{entity.cancelledAt},
                cancelled_by = #{entity.cancelledBy},
                cancel_reason = #{entity.cancelReason},
                updated_by = #{actorId}
            WHERE id = #{entity.id}
              AND is_deleted = FALSE
            """)
    int updateActionState(@Param("entity") PurchaseOrderDbEntity entity, @Param("actorId") UUID actorId);

    @Insert("""
            INSERT INTO finance.po_line_items (
                id, po_id, line_number, rfq_line_item_id, pr_line_item_id, item_name,
                category_code, quantity, unit, unit_price, total_price, currency,
                delivery_days, warranty, created_by
            ) VALUES (
                #{entity.id}, #{entity.poId}, #{entity.lineNumber}, #{entity.rfqLineItemId},
                #{entity.prLineItemId}, #{entity.itemName}, #{entity.categoryCode},
                #{entity.quantity}, #{entity.unit}, #{entity.unitPrice}, #{entity.totalPrice},
                #{entity.currency}, #{entity.deliveryDays}, #{entity.warranty}, #{createdBy}
            )
            """)
    void insertLineItem(@Param("entity") PurchaseOrderLineItemDbEntity entity, @Param("createdBy") UUID createdBy);

    @Select("""
            SELECT EXISTS (
                SELECT 1
                FROM finance.event_processing_log
                WHERE event_id = #{eventId}
            )
            """)
    boolean existsProcessedEvent(@Param("eventId") String eventId);

    @Insert("""
            INSERT INTO finance.event_processing_log (
                event_id, topic, partition_id, offset_value, handler_name, status
            ) VALUES (
                #{eventId}, #{topic}, #{partitionId}, #{offsetValue}, #{handlerName}, 'PROCESSED'
            )
            ON CONFLICT (event_id) DO NOTHING
            """)
    void markEventProcessed(
            @Param("eventId") String eventId,
            @Param("topic") String topic,
            @Param("partitionId") Integer partitionId,
            @Param("offsetValue") Long offsetValue,
            @Param("handlerName") String handlerName);
}
