package com.eprocure.pr.infrastructure.persistence.mapper;

import com.eprocure.pr.infrastructure.persistence.entity.CatalogCategoryAdminDbEntity;
import com.eprocure.pr.infrastructure.persistence.entity.CatalogCategoryDbEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CatalogCategoryMapper {

    @Select("SELECT * FROM pr.catalog_categories WHERE is_deleted = false ORDER BY code ASC")
    List<CatalogCategoryDbEntity> findAll();

    @Select("""
            <script>
            SELECT c.code,
                   c.name,
                   c.parent_code,
                   c.requires_special_approval,
                   c.special_approver_role,
                   c.requires_rfq_above,
                   c.currency,
                   c.is_capex,
                   c.is_deleted,
                   COUNT(i.id) AS item_count
            FROM pr.catalog_categories c
            LEFT JOIN pr.catalog_items i
              ON i.category_code = c.code
             AND i.is_deleted = FALSE
             AND i.is_active = TRUE
            WHERE 1 = 1
            <if test="includeInactive == false">
              AND c.is_deleted = FALSE
            </if>
            GROUP BY c.code,
                     c.name,
                     c.parent_code,
                     c.requires_special_approval,
                     c.special_approver_role,
                     c.requires_rfq_above,
                     c.currency,
                     c.is_capex,
                     c.is_deleted
            ORDER BY c.code ASC
            </script>
            """)
    List<CatalogCategoryAdminDbEntity> findAdminCategories(@Param("includeInactive") boolean includeInactive);

    @Select("""
            SELECT c.code,
                   c.name,
                   c.parent_code,
                   c.requires_special_approval,
                   c.special_approver_role,
                   c.requires_rfq_above,
                   c.currency,
                   c.is_capex,
                   c.is_deleted,
                   COUNT(i.id) AS item_count
            FROM pr.catalog_categories c
            LEFT JOIN pr.catalog_items i
              ON i.category_code = c.code
             AND i.is_deleted = FALSE
             AND i.is_active = TRUE
            WHERE c.code = #{code}
            GROUP BY c.code,
                     c.name,
                     c.parent_code,
                     c.requires_special_approval,
                     c.special_approver_role,
                     c.requires_rfq_above,
                     c.currency,
                     c.is_capex,
                     c.is_deleted
            LIMIT 1
            """)
    CatalogCategoryAdminDbEntity findAdminByCode(@Param("code") String code);

    @Select("""
            SELECT COUNT(1) > 0
            FROM pr.catalog_categories
            WHERE code = #{code}
            """)
    boolean existsByCode(@Param("code") String code);

    @Select("""
            SELECT COUNT(1) > 0
            FROM pr.catalog_categories
            WHERE code = #{code}
              AND is_deleted = FALSE
            """)
    boolean existsActiveByCode(@Param("code") String code);

    @Select("""
            SELECT COUNT(1)
            FROM pr.catalog_items
            WHERE category_code = #{categoryCode}
              AND is_active = TRUE
              AND is_deleted = FALSE
            """)
    long countActiveItems(@Param("categoryCode") String categoryCode);

    @Insert("""
            INSERT INTO pr.catalog_categories (
                code,
                name,
                parent_code,
                requires_special_approval,
                special_approver_role,
                requires_rfq_above,
                currency,
                is_capex,
                created_by,
                updated_at
            )
            VALUES (
                #{code},
                #{name},
                #{parentCode,jdbcType=VARCHAR},
                #{requiresSpecialApproval},
                #{specialApproverRole,jdbcType=VARCHAR},
                #{requiresRfqAbove,jdbcType=NUMERIC},
                'VND',
                #{capex},
                #{actorId},
                NOW()
            )
            """)
    void insert(
            @Param("code") String code,
            @Param("name") String name,
            @Param("parentCode") String parentCode,
            @Param("requiresSpecialApproval") boolean requiresSpecialApproval,
            @Param("specialApproverRole") String specialApproverRole,
            @Param("requiresRfqAbove") BigDecimal requiresRfqAbove,
            @Param("capex") boolean capex,
            @Param("actorId") UUID actorId);

    @Update("""
            UPDATE pr.catalog_categories
            SET name = #{name},
                parent_code = #{parentCode,jdbcType=VARCHAR},
                requires_special_approval = #{requiresSpecialApproval},
                special_approver_role = #{specialApproverRole,jdbcType=VARCHAR},
                requires_rfq_above = #{requiresRfqAbove,jdbcType=NUMERIC},
                currency = 'VND',
                is_capex = #{capex},
                updated_by = #{actorId},
                updated_at = NOW()
            WHERE code = #{code}
              AND is_deleted = FALSE
            """)
    void update(
            @Param("code") String code,
            @Param("name") String name,
            @Param("parentCode") String parentCode,
            @Param("requiresSpecialApproval") boolean requiresSpecialApproval,
            @Param("specialApproverRole") String specialApproverRole,
            @Param("requiresRfqAbove") BigDecimal requiresRfqAbove,
            @Param("capex") boolean capex,
            @Param("actorId") UUID actorId);

    @Update("""
            UPDATE pr.catalog_categories
            SET is_deleted = TRUE,
                deleted_at = #{deletedAt},
                deleted_by = #{actorId},
                updated_by = #{actorId},
                updated_at = NOW()
            WHERE code = #{code}
              AND is_deleted = FALSE
            """)
    void deactivate(
            @Param("code") String code,
            @Param("actorId") UUID actorId,
            @Param("deletedAt") Instant deletedAt);

}
