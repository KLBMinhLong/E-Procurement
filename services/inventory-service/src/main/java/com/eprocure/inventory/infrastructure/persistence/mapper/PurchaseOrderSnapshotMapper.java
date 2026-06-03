package com.eprocure.inventory.infrastructure.persistence.mapper;

import com.eprocure.inventory.infrastructure.persistence.entity.PurchaseOrderLineSnapshotDbEntity;
import com.eprocure.inventory.infrastructure.persistence.entity.PurchaseOrderSnapshotDbEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PurchaseOrderSnapshotMapper {
    Optional<PurchaseOrderSnapshotDbEntity> findHeaderByPoId(@Param("poId") UUID poId);

    Optional<PurchaseOrderSnapshotDbEntity> findHeaderBySourceEventId(@Param("sourceEventId") String sourceEventId);

    List<PurchaseOrderLineSnapshotDbEntity> findLineItemsBySnapshotId(@Param("snapshotId") UUID snapshotId);

    @Insert("""
            INSERT INTO inventory.purchase_order_snapshots (
                id, po_id, po_number, pr_id, pr_number, vendor_id, vendor_name,
                vendor_email, vendor_tax_code, purchasing_officer_id, total_amount,
                currency, delivery_address, delivery_deadline, payment_terms,
                issued_at, sent_to_vendor_at, created_at, created_by, source_event_id
            ) VALUES (
                #{entity.id}, #{entity.poId}, #{entity.poNumber}, #{entity.prId}, #{entity.prNumber},
                #{entity.vendorId}, #{entity.vendorName}, #{entity.vendorEmail}, #{entity.vendorTaxCode},
                #{entity.purchasingOfficerId}, #{entity.totalAmount}, #{entity.currency},
                #{entity.deliveryAddress}, #{entity.deliveryDeadline}, #{entity.paymentTerms},
                #{entity.issuedAt}, #{entity.sentToVendorAt}, #{entity.createdAt},
                #{entity.purchasingOfficerId}, #{entity.sourceEventId}
            )
            """)
    void insertSnapshot(@Param("entity") PurchaseOrderSnapshotDbEntity entity);

    @Insert("""
            INSERT INTO inventory.purchase_order_line_snapshots (
                snapshot_id, po_id, po_line_item_id, pr_line_item_id, item_name,
                category_code, quantity, unit, unit_price, total_price, currency, created_by
            ) VALUES (
                #{entity.snapshotId}, #{entity.poId}, #{entity.poLineItemId}, #{entity.prLineItemId},
                #{entity.itemName}, #{entity.categoryCode}, #{entity.quantity}, #{entity.unit},
                #{entity.unitPrice}, #{entity.totalPrice}, #{entity.currency}, #{entity.createdBy}
            )
            """)
    void insertLineItem(@Param("entity") PurchaseOrderLineSnapshotDbEntity entity);

    @Select("""
            SELECT EXISTS (
                SELECT 1
                FROM inventory.event_processing_log
                WHERE event_id = #{eventId}
            )
            """)
    boolean existsProcessedEvent(@Param("eventId") String eventId);

    @Insert("""
            INSERT INTO inventory.event_processing_log (
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
