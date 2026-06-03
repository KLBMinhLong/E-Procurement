package com.eprocure.inventory.infrastructure.persistence.mapper;

import com.eprocure.inventory.domain.repository.StockEntryFilter;
import com.eprocure.inventory.domain.repository.StockMovementFilter;
import com.eprocure.inventory.infrastructure.persistence.entity.StockIssueOutRequestDbEntity;
import com.eprocure.inventory.infrastructure.persistence.entity.StockEntryViewDbEntity;
import com.eprocure.inventory.infrastructure.persistence.entity.StockMovementDbEntity;
import com.eprocure.inventory.infrastructure.persistence.entity.StockMovementHistoryDbEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface StockMapper {
    @Select("""
            SELECT EXISTS (
                SELECT 1
                FROM inventory.items
                WHERE item_code = #{itemCode}
                  AND is_active = TRUE
                  AND is_deleted = FALSE
            )
            """)
    boolean existsActiveItem(@Param("itemCode") String itemCode);

    @Select("""
            SELECT EXISTS (
                SELECT 1
                FROM inventory.warehouses
                WHERE id = #{warehouseId}
                  AND is_active = TRUE
                  AND is_deleted = FALSE
            )
            """)
    boolean existsActiveWarehouse(@Param("warehouseId") UUID warehouseId);

    List<StockEntryViewDbEntity> findStockEntries(@Param("filter") StockEntryFilter filter);

    long countStockEntries(@Param("filter") StockEntryFilter filter);

    List<StockMovementHistoryDbEntity> findMovements(@Param("filter") StockMovementFilter filter);

    long countMovements(@Param("filter") StockMovementFilter filter);

    Optional<StockIssueOutRequestDbEntity> findIssueOutRequestByIdempotencyKey(
            @Param("idempotencyKey") UUID idempotencyKey);

    @Insert("""
            INSERT INTO inventory.stock_issue_out_requests (
                id, idempotency_key, warehouse_id, pr_id, recipient_id,
                issued_by, issued_at, notes, created_by, updated_by
            ) VALUES (
                #{entity.id}, #{entity.idempotencyKey}, #{entity.warehouseId},
                #{entity.prId}, #{entity.recipientId}, #{entity.issuedBy},
                #{entity.issuedAt}, #{entity.notes}, #{entity.issuedBy}, #{entity.issuedBy}
            )
            """)
    void insertIssueOutRequest(@Param("entity") StockIssueOutRequestDbEntity entity);

    @Select("""
            UPDATE inventory.stock_entries
            SET quantity_on_hand = quantity_on_hand - #{quantity},
                last_updated = #{occurredAt},
                updated_by = #{actorId},
                updated_at = NOW()
            WHERE item_code = #{itemCode}
              AND warehouse_id = #{warehouseId}
              AND unit = #{unit}
              AND quantity_on_hand >= #{quantity}
              AND is_deleted = FALSE
            RETURNING quantity_on_hand
            """)
    Optional<BigDecimal> issueStock(
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

    List<StockMovementHistoryDbEntity> findMovementsBySource(
            @Param("sourceRefType") String sourceRefType,
            @Param("sourceRefId") UUID sourceRefId);
}
