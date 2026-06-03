package com.eprocure.inventory.infrastructure.persistence.mapper;

import com.eprocure.inventory.domain.repository.StockEntryFilter;
import com.eprocure.inventory.domain.repository.StockMovementFilter;
import com.eprocure.inventory.infrastructure.persistence.entity.StockEntryViewDbEntity;
import com.eprocure.inventory.infrastructure.persistence.entity.StockMovementHistoryDbEntity;
import java.util.List;
import java.util.UUID;
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
}
