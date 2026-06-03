package com.eprocure.inventory.infrastructure.persistence.mapper;

import com.eprocure.inventory.domain.repository.GoodsReceiptFilter;
import com.eprocure.inventory.infrastructure.persistence.entity.GoodsReceiptDbEntity;
import com.eprocure.inventory.infrastructure.persistence.entity.GoodsReceiptLineItemDbEntity;
import com.eprocure.inventory.infrastructure.persistence.entity.WarehouseSnapshotDbEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface GoodsReceiptMapper {
    Optional<GoodsReceiptDbEntity> findHeaderById(@Param("id") UUID id);

    Optional<GoodsReceiptDbEntity> findHeaderByIdempotencyKey(@Param("idempotencyKey") UUID idempotencyKey);

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
}
