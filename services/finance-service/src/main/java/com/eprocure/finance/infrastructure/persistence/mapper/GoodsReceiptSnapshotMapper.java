package com.eprocure.finance.infrastructure.persistence.mapper;

import com.eprocure.finance.infrastructure.persistence.entity.GoodsReceiptLineSnapshotDbEntity;
import com.eprocure.finance.infrastructure.persistence.entity.ReceivedQuantityDbEntity;
import com.eprocure.finance.infrastructure.persistence.entity.GoodsReceiptSnapshotDbEntity;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface GoodsReceiptSnapshotMapper {
    @Insert("""
            INSERT INTO finance.goods_receipt_snapshots (
                id, gr_number, po_id, po_number, warehouse_id, warehouse_keeper_id,
                status, received_at, completed_at, source_event_id, created_by, updated_by
            ) VALUES (
                #{entity.id}, #{entity.grNumber}, #{entity.poId}, #{entity.poNumber},
                #{entity.warehouseId}, #{entity.warehouseKeeperId}, #{entity.status},
                #{entity.receivedAt}, #{entity.completedAt}, #{entity.sourceEventId},
                #{entity.warehouseKeeperId}, #{entity.warehouseKeeperId}
            )
            ON CONFLICT (id) DO UPDATE
            SET status = EXCLUDED.status,
                received_at = EXCLUDED.received_at,
                completed_at = EXCLUDED.completed_at,
                updated_by = EXCLUDED.updated_by
            WHERE finance.goods_receipt_snapshots.is_deleted = FALSE
            """)
    void upsertHeader(@Param("entity") GoodsReceiptSnapshotDbEntity entity);

    @Insert("""
            INSERT INTO finance.goods_receipt_line_snapshots (
                gr_line_item_id, gr_id, po_line_item_id, item_code, item_name,
                ordered_quantity, received_quantity, rejected_quantity, unit, created_by, updated_by
            ) VALUES (
                #{entity.grLineItemId}, #{entity.grId}, #{entity.poLineItemId}, #{entity.itemCode},
                #{entity.itemName}, #{entity.orderedQuantity}, #{entity.receivedQuantity},
                #{entity.rejectedQuantity}, #{entity.unit}, #{actorId}, #{actorId}
            )
            ON CONFLICT (gr_line_item_id) DO UPDATE
            SET item_code = EXCLUDED.item_code,
                item_name = EXCLUDED.item_name,
                ordered_quantity = EXCLUDED.ordered_quantity,
                received_quantity = EXCLUDED.received_quantity,
                rejected_quantity = EXCLUDED.rejected_quantity,
                unit = EXCLUDED.unit,
                updated_by = EXCLUDED.updated_by
            WHERE finance.goods_receipt_line_snapshots.is_deleted = FALSE
            """)
    void upsertLineItem(
            @Param("entity") GoodsReceiptLineSnapshotDbEntity entity,
            @Param("actorId") UUID actorId);

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

    @Select("""
            SELECT
                gls.po_line_item_id AS poLineItemId,
                COALESCE(SUM(gls.received_quantity), 0) AS receivedQuantity
            FROM finance.goods_receipt_line_snapshots gls
            JOIN finance.goods_receipt_snapshots grs
              ON grs.id = gls.gr_id
             AND grs.is_deleted = FALSE
            WHERE gls.is_deleted = FALSE
              AND grs.po_id = #{poId}
              AND grs.status IN ('PARTIAL', 'COMPLETE', 'DISCREPANCY')
            GROUP BY gls.po_line_item_id
            """)
    List<ReceivedQuantityDbEntity> findReceivedQuantitiesByPoId(@Param("poId") UUID poId);
}
