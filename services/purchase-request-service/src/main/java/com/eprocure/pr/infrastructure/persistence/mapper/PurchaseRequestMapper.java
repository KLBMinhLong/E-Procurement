package com.eprocure.pr.infrastructure.persistence.mapper;

import com.eprocure.pr.infrastructure.persistence.entity.PrLineItemDbEntity;
import com.eprocure.pr.infrastructure.persistence.entity.PurchaseRequestDbEntity;
import com.eprocure.pr.infrastructure.persistence.typehandler.InventoryCheckResultTypeHandler;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PurchaseRequestMapper {

    @Select("SELECT nextval('pr.pr_number_sequence')")
    long nextPrNumberSequence();

    @Insert("""
            INSERT INTO pr.purchase_requests (
                id, pr_number, requester_id, department_id, title, justification,
                priority, urgency_reason, status, total_amount, currency, fiscal_year,
                need_by_date, related_contract_id, is_blanket_release, submitted_at,
                created_at, updated_at, created_by, updated_by, is_deleted, deleted_at, deleted_by
            ) VALUES (
                #{id}, #{prNumber}, #{requesterId}, #{departmentId}, #{title}, #{justification},
                #{priority}, #{urgencyReason}, #{status}, #{totalAmount.amount}, #{totalAmount.currency}, #{fiscalYear},
                #{needByDate}, #{relatedContractId}, #{blanketRelease}, #{submittedAt},
                #{createdAt}, #{updatedAt}, #{createdBy}, #{updatedBy}, #{deleted}, #{deletedAt}, #{deletedBy}
            )
            """)
    void insert(PurchaseRequestDbEntity entity);

    @Insert("""
            INSERT INTO pr.pr_line_items (
                id, pr_id, line_number, item_code, item_name, description, category_code,
                quantity, unit, unit_price, currency, preferred_vendor_id, specifications,
                gl_account_code, is_from_catalog, created_by, is_deleted
            ) VALUES (
                #{entity.id}, #{entity.purchaseRequestId}, #{entity.lineNumber}, #{entity.itemCode},
                #{entity.itemName}, #{entity.description}, #{entity.categoryCode}, #{entity.quantity.amount},
                #{entity.quantity.unit}, #{entity.unitPrice.amount}, #{entity.unitPrice.currency},
                #{entity.preferredVendorId}, #{entity.specifications}, #{entity.glAccountCode},
                #{entity.fromCatalog}, #{createdBy}, false
            )
            """)
    void insertLineItem(@Param("entity") PrLineItemDbEntity entity, @Param("createdBy") UUID createdBy);

    @Update("""
            UPDATE pr.purchase_requests
            SET title = #{title},
                justification = #{justification},
                priority = #{priority},
                urgency_reason = #{urgencyReason},
                status = #{status},
                total_amount = #{totalAmount.amount},
                currency = #{totalAmount.currency},
                fiscal_year = #{fiscalYear},
                need_by_date = #{needByDate},
                related_contract_id = #{relatedContractId},
                is_blanket_release = #{blanketRelease},
                budget_allocated = #{budgetAllocatedAmount},
                budget_committed = #{budgetCommittedAmount},
                budget_spent = #{budgetSpentAmount},
                budget_available = #{budgetAvailableAmount},
                budget_check_status = #{budgetCheckStatus},
                budget_warning_message = #{budgetWarningMessage},
                inventory_check = #{inventoryCheck,jdbcType=OTHER,typeHandler=com.eprocure.pr.infrastructure.persistence.typehandler.InventoryCheckResultTypeHandler},
                submitted_at = #{submittedAt},
                updated_at = #{updatedAt},
                updated_by = #{updatedBy}
            WHERE id = #{id} AND is_deleted = false
            """)
    void updateRoot(PurchaseRequestDbEntity entity);

    @Select("""
            SELECT
                id, pr_number, requester_id, department_id, title, justification, priority,
                urgency_reason, status, total_amount, currency, fiscal_year, need_by_date,
                related_contract_id, is_blanket_release, budget_allocated, budget_committed,
                budget_spent, budget_available, budget_check_status, budget_warning_message, inventory_check,
                submitted_at, created_at, updated_at,
                created_by, updated_by, is_deleted, deleted_at, deleted_by
            FROM pr.purchase_requests
            WHERE id = #{id} AND is_deleted = false
            """)
    @Results(id = "purchaseRequestResult", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "prNumber", column = "pr_number"),
            @Result(property = "requesterId", column = "requester_id"),
            @Result(property = "departmentId", column = "department_id"),
            @Result(property = "urgencyReason", column = "urgency_reason"),
            @Result(property = "totalAmountAmount", column = "total_amount"),
            @Result(property = "totalAmountCurrency", column = "currency"),
            @Result(property = "fiscalYear", column = "fiscal_year"),
            @Result(property = "needByDate", column = "need_by_date"),
            @Result(property = "relatedContractId", column = "related_contract_id"),
            @Result(property = "blanketRelease", column = "is_blanket_release"),
            @Result(property = "budgetAllocatedAmount", column = "budget_allocated"),
            @Result(property = "budgetCommittedAmount", column = "budget_committed"),
            @Result(property = "budgetSpentAmount", column = "budget_spent"),
            @Result(property = "budgetAvailableAmount", column = "budget_available"),
            @Result(property = "budgetCheckStatus", column = "budget_check_status"),
            @Result(property = "budgetWarningMessage", column = "budget_warning_message"),
            @Result(property = "inventoryCheck", column = "inventory_check", typeHandler = InventoryCheckResultTypeHandler.class),
            @Result(property = "submittedAt", column = "submitted_at"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "createdBy", column = "created_by"),
            @Result(property = "updatedBy", column = "updated_by"),
            @Result(property = "deleted", column = "is_deleted"),
            @Result(property = "deletedAt", column = "deleted_at"),
            @Result(property = "deletedBy", column = "deleted_by")
    })
    Optional<PurchaseRequestDbEntity> findById(UUID id);

    @Select("""
            SELECT
                id, pr_number, requester_id, department_id, title, justification, priority,
                urgency_reason, status, total_amount, currency, fiscal_year, need_by_date,
                related_contract_id, is_blanket_release, budget_allocated, budget_committed,
                budget_spent, budget_available, budget_check_status, budget_warning_message, inventory_check,
                submitted_at, created_at, updated_at,
                created_by, updated_by, is_deleted, deleted_at, deleted_by
            FROM pr.purchase_requests
            WHERE pr_number = #{prNumber} AND is_deleted = false
            """)
    @Results(id = "purchaseRequestByNumberResult", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "prNumber", column = "pr_number"),
            @Result(property = "requesterId", column = "requester_id"),
            @Result(property = "departmentId", column = "department_id"),
            @Result(property = "urgencyReason", column = "urgency_reason"),
            @Result(property = "totalAmountAmount", column = "total_amount"),
            @Result(property = "totalAmountCurrency", column = "currency"),
            @Result(property = "fiscalYear", column = "fiscal_year"),
            @Result(property = "needByDate", column = "need_by_date"),
            @Result(property = "relatedContractId", column = "related_contract_id"),
            @Result(property = "blanketRelease", column = "is_blanket_release"),
            @Result(property = "budgetAllocatedAmount", column = "budget_allocated"),
            @Result(property = "budgetCommittedAmount", column = "budget_committed"),
            @Result(property = "budgetSpentAmount", column = "budget_spent"),
            @Result(property = "budgetAvailableAmount", column = "budget_available"),
            @Result(property = "budgetCheckStatus", column = "budget_check_status"),
            @Result(property = "budgetWarningMessage", column = "budget_warning_message"),
            @Result(property = "inventoryCheck", column = "inventory_check", typeHandler = InventoryCheckResultTypeHandler.class),
            @Result(property = "submittedAt", column = "submitted_at"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "createdBy", column = "created_by"),
            @Result(property = "updatedBy", column = "updated_by"),
            @Result(property = "deleted", column = "is_deleted"),
            @Result(property = "deletedAt", column = "deleted_at"),
            @Result(property = "deletedBy", column = "deleted_by")
    })
    Optional<PurchaseRequestDbEntity> findByPrNumber(String prNumber);

    @Select("""
            SELECT
                id, pr_id, line_number, item_code, item_name, description, category_code,
                quantity, unit, unit_price, total_price, currency, preferred_vendor_id,
                specifications, gl_account_code, is_from_catalog
            FROM pr.pr_line_items
            WHERE pr_id = #{purchaseRequestId} AND is_deleted = false
            ORDER BY line_number ASC
            """)
    @Results(id = "lineItemResult", value = {
            @Result(property = "purchaseRequestId", column = "pr_id"),
            @Result(property = "lineNumber", column = "line_number"),
            @Result(property = "itemCode", column = "item_code"),
            @Result(property = "itemName", column = "item_name"),
            @Result(property = "categoryCode", column = "category_code"),
            @Result(property = "quantityAmount", column = "quantity"),
            @Result(property = "quantityUnit", column = "unit"),
            @Result(property = "unitPriceAmount", column = "unit_price"),
            @Result(property = "unitPriceCurrency", column = "currency"),
            @Result(property = "totalPriceAmount", column = "total_price"),
            @Result(property = "totalPriceCurrency", column = "currency"),
            @Result(property = "preferredVendorId", column = "preferred_vendor_id"),
            @Result(property = "glAccountCode", column = "gl_account_code"),
            @Result(property = "fromCatalog", column = "is_from_catalog")
    })
    List<PrLineItemDbEntity> findLineItems(UUID purchaseRequestId);

    @Select("""
            SELECT EXISTS (
                SELECT 1
                FROM pr.purchase_requests
                WHERE pr_number = #{prNumber} AND is_deleted = false
            )
            """)
    boolean existsByPrNumber(String prNumber);

    @Update("""
            UPDATE pr.purchase_requests
            SET is_deleted = true,
                deleted_at = #{deletedAt},
                deleted_by = #{deletedBy},
                updated_at = #{deletedAt}
            WHERE id = #{id} AND is_deleted = false
            """)
    void softDelete(@Param("id") UUID id, @Param("deletedBy") UUID deletedBy, @Param("deletedAt") Instant deletedAt);

    /**
     * Soft-delete all line items belonging to a PR.
     * Called before re-inserting new line items in UpdatePR.
     */
    @Update("""
            UPDATE pr.pr_line_items
            SET is_deleted = true
            WHERE pr_id = #{prId} AND is_deleted = false
            """)
    void deleteLineItemsByPrId(UUID prId);

    // ── List / filter queries (complex dynamic SQL → XML mapper) ─────────────

    /**
     * Paginated list without loading line items (lightweight for list view).
     * Dynamic WHERE implemented in PurchaseRequestMapper.xml.
     */
    List<PurchaseRequestDbEntity> findByFilter(@Param("filter") com.eprocure.pr.domain.repository.PurchaseRequestFilter filter);

    /**
     * Count matching rows for pagination meta.
     */
    long countByFilter(@Param("filter") com.eprocure.pr.domain.repository.PurchaseRequestFilter filter);
}
