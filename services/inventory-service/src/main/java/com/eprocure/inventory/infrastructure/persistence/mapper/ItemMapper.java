package com.eprocure.inventory.infrastructure.persistence.mapper;

import com.eprocure.inventory.domain.repository.ItemFilter;
import com.eprocure.inventory.infrastructure.persistence.entity.CatalogItemMutationRequestDbEntity;
import com.eprocure.inventory.infrastructure.persistence.entity.ItemDbEntity;
import com.eprocure.inventory.infrastructure.persistence.entity.ItemStockSummaryDbEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ItemMapper {
    Optional<ItemDbEntity> findByCode(@Param("itemCode") String itemCode);

    List<ItemDbEntity> findByFilter(@Param("filter") ItemFilter filter);

    long countByFilter(@Param("filter") ItemFilter filter);

    List<ItemStockSummaryDbEntity> findStockSummary(@Param("itemCode") String itemCode);

    @Select("""
            SELECT EXISTS (
                SELECT 1
                FROM inventory.items
                WHERE item_code = #{itemCode}
                  AND is_deleted = FALSE
            )
            """)
    boolean existsActiveCode(@Param("itemCode") String itemCode);

    Optional<CatalogItemMutationRequestDbEntity> findMutationRequestByIdempotencyKey(
            @Param("idempotencyKey") UUID idempotencyKey);

    @Insert("""
            INSERT INTO inventory.items (
                id, item_code, name, description, category_code, unit,
                unit_price, currency, preferred_vendor_id, reorder_point,
                is_active, created_at, created_by, updated_by, is_deleted
            ) VALUES (
                #{entity.id}, #{entity.itemCode}, #{entity.name}, #{entity.description},
                #{entity.categoryCode}, #{entity.unit}, #{entity.unitPrice}, #{entity.currency},
                #{entity.preferredVendorId}, #{entity.reorderPoint}, #{entity.active},
                #{entity.createdAt}, #{entity.createdBy}, #{entity.updatedBy}, FALSE
            )
            """)
    void insert(@Param("entity") ItemDbEntity entity);

    @Update("""
            UPDATE inventory.items
            SET name = #{entity.name},
                description = #{entity.description},
                unit_price = #{entity.unitPrice},
                currency = #{entity.currency},
                preferred_vendor_id = #{entity.preferredVendorId},
                reorder_point = #{entity.reorderPoint},
                is_active = #{entity.active},
                updated_by = #{entity.updatedBy},
                updated_at = NOW()
            WHERE item_code = #{entity.itemCode}
              AND is_deleted = FALSE
            """)
    int update(@Param("entity") ItemDbEntity entity);

    @Insert("""
            INSERT INTO inventory.catalog_item_mutation_requests (
                id, idempotency_key, operation, item_code, actor_id, created_at
            ) VALUES (
                #{entity.id}, #{entity.idempotencyKey}, #{entity.operation},
                #{entity.itemCode}, #{entity.actorId}, #{entity.createdAt}
            )
            """)
    void insertMutationRequest(@Param("entity") CatalogItemMutationRequestDbEntity entity);
}
