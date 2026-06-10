package com.eprocure.inventory.infrastructure.persistence.mapper;

import com.eprocure.inventory.domain.repository.GoodsReceiptFilter;
import com.eprocure.inventory.domain.model.GoodsReceiptStatus;
import com.eprocure.inventory.infrastructure.persistence.entity.GoodsReceiptDbEntity;
import com.eprocure.inventory.infrastructure.persistence.entity.GoodsReceiptLineItemDbEntity;
import com.eprocure.inventory.infrastructure.persistence.entity.StockBalanceDbEntity;
import com.eprocure.inventory.infrastructure.persistence.entity.StockMovementDbEntity;
import com.eprocure.inventory.infrastructure.persistence.entity.WarehouseListDbEntity;
import com.eprocure.inventory.infrastructure.persistence.entity.WarehouseSnapshotDbEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface GoodsReceiptMapper {
    Optional<GoodsReceiptDbEntity> findHeaderById(@Param("id") UUID id);

    Optional<GoodsReceiptDbEntity> findHeaderByIdempotencyKey(@Param("idempotencyKey") UUID idempotencyKey);

    Optional<GoodsReceiptDbEntity> findHeaderByIdAndCompleteIdempotencyKey(
            @Param("id") UUID id,
            @Param("idempotencyKey") UUID idempotencyKey);

    List<GoodsReceiptLineItemDbEntity> findLineItemsByGoodsReceiptId(@Param("goodsReceiptId") UUID goodsReceiptId);

    List<GoodsReceiptDbEntity> findHeadersByFilter(@Param("filter") GoodsReceiptFilter filter);

    long countByFilter(@Param("filter") GoodsReceiptFilter filter);

    @Select("""
            SELECT 'GR-' || CAST(#{fiscalYear} AS text) || '-' || LPAD(nextval('inventory.gr_number_seq')::text, 6, '0')
            """)
    String nextGrNumber(@Param("fiscalYear") int fiscalYear);

    @Select("""
            SELECT id, name
            FROM inventory.warehouses
            WHERE id = #{warehouseId}
              AND is_active = TRUE
              AND is_deleted = FALSE
            """)
    Optional<WarehouseSnapshotDbEntity> findActiveWarehouseById(@Param("warehouseId") UUID warehouseId);

    @Select("""
            SELECT id, code, name
            FROM inventory.warehouses
            WHERE is_active = TRUE
              AND is_deleted = FALSE
            ORDER BY name ASC
            """)
    List<WarehouseListDbEntity> findActiveWarehouses();

    @Select("""
            SELECT item.item_code
            FROM inventory.purchase_order_line_snapshots po_line
            JOIN inventory.items item
              ON lower(item.name) = lower(po_line.item_name)
             AND item.category_code = po_line.category_code
             AND item.unit = po_line.unit
             AND item.is_active = TRUE
             AND item.is_deleted = FALSE
            WHERE po_line.po_line_item_id = #{poLineItemId}
              AND po_line.is_deleted = FALSE
            ORDER BY item.item_code ASC
            LIMIT 1
            """)
    Optional<String> findActiveItemCodeForPoLineItem(@Param("poLineItemId") UUID poLineItemId);

    @Insert("""
            INSERT INTO inventory.goods_receipts (
                id, gr_number, po_id, warehouse_id, warehouse_keeper_id,
                warehouse_keeper_full_name, received_at, status, notes,
                created_at, created_by, updated_by, idempotency_key
            ) VALUES (
                #{entity.id}, #{entity.grNumber}, #{entity.poId}, #{entity.warehouseId},
                #{entity.warehouseKeeperId}, #{entity.warehouseKeeperFullName},
                #{entity.receivedAt}, #{entity.status}, #{entity.notes},
                #{entity.createdAt}, #{entity.createdBy}, #{entity.updatedBy}, #{entity.idempotencyKey}
            )
            """)
    void insertHeader(@Param("entity") GoodsReceiptDbEntity entity);

    @Insert("""
            INSERT INTO inventory.goods_receipt_line_items (
                id, goods_receipt_id, po_line_item_id, item_code, item_name,
                ordered_quantity, received_quantity, rejected_quantity, unit,
                rejection_reason, lot_number, created_by
            ) VALUES (
                #{entity.id}, #{entity.goodsReceiptId}, #{entity.poLineItemId}, #{entity.itemCode},
                #{entity.itemName}, #{entity.orderedQuantity}, #{entity.receivedQuantity},
                #{entity.rejectedQuantity}, #{entity.unit}, #{entity.rejectionReason},
                #{entity.lotNumber}, #{entity.createdBy}
            )
            """)
    void insertLineItem(@Param("entity") GoodsReceiptLineItemDbEntity entity);

    @Update("""
            UPDATE inventory.goods_receipt_line_items
            SET item_code = #{itemCode},
                updated_by = #{actorId}
            WHERE id = #{lineItemId}
              AND is_deleted = FALSE
            """)
    void updateLineItemCode(
            @Param("lineItemId") UUID lineItemId,
            @Param("itemCode") String itemCode,
            @Param("actorId") UUID actorId);

    @Select("""
            WITH upserted AS (
                INSERT INTO inventory.stock_entries (
                    item_code, warehouse_id, quantity_on_hand, unit,
                    last_updated, created_by, updated_by, is_deleted
                ) VALUES (
                    #{itemCode}, #{warehouseId}, #{quantity}, #{unit},
                    #{occurredAt}, #{actorId}, #{actorId}, FALSE
                )
                ON CONFLICT (item_code, warehouse_id) WHERE is_deleted = FALSE
                DO UPDATE SET
                    quantity_on_hand = inventory.stock_entries.quantity_on_hand + EXCLUDED.quantity_on_hand,
                    unit = EXCLUDED.unit,
                    last_updated = EXCLUDED.last_updated,
                    updated_by = EXCLUDED.updated_by,
                    updated_at = NOW()
                RETURNING quantity_on_hand
            )
            SELECT quantity_on_hand
            FROM upserted
            """)
    BigDecimal receiveStock(
            @Param("itemCode") String itemCode,
            @Param("warehouseId") UUID warehouseId,
            @Param("quantity") BigDecimal quantity,
            @Param("unit") String unit,
            @Param("actorId") UUID actorId,
            @Param("occurredAt") Instant occurredAt);

    @Insert("""
            INSERT INTO inventory.stock_movements (
                id, item_code, warehouse_id, movement_type, quantity, unit,
                balance_after, source_ref_type, source_ref_id,
                performed_by, performed_at, notes
            ) VALUES (
                #{entity.id}, #{entity.itemCode}, #{entity.warehouseId},
                #{entity.movementType}, #{entity.quantity}, #{entity.unit},
                #{entity.balanceAfter}, #{entity.sourceRefType}, #{entity.sourceRefId},
                #{entity.performedBy}, #{entity.performedAt}, #{entity.notes}
            )
            """)
    void insertStockMovement(@Param("entity") StockMovementDbEntity entity);

    @Update("""
            UPDATE inventory.goods_receipts
            SET status = #{status},
                updated_by = #{actorId},
                completed_at = #{completedAt},
                completed_by = #{actorId},
                completed_idempotency_key = #{idempotencyKey}
            WHERE id = #{id}
              AND status = 'DRAFT'
              AND is_deleted = FALSE
            """)
    int markCompleted(
            @Param("id") UUID id,
            @Param("status") GoodsReceiptStatus status,
            @Param("actorId") UUID actorId,
            @Param("completedAt") Instant completedAt,
            @Param("idempotencyKey") UUID idempotencyKey);

    @Select("""
            SELECT COUNT(*)
            FROM inventory.stock_movements
            WHERE source_ref_type = 'GOODS_RECEIPT'
              AND source_ref_id = #{goodsReceiptId}
            """)
    int countReceiptMovements(@Param("goodsReceiptId") UUID goodsReceiptId);

    List<StockBalanceDbEntity> findStockBalancesByReceipt(@Param("goodsReceiptId") UUID goodsReceiptId);
}
