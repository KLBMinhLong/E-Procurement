package com.eprocure.finance.infrastructure.persistence.mapper;

import com.eprocure.finance.domain.model.BudgetTransactionType;
import com.eprocure.finance.domain.repository.BudgetFilter;
import com.eprocure.finance.infrastructure.persistence.entity.BudgetCommitmentHoldDbEntity;
import com.eprocure.finance.infrastructure.persistence.entity.BudgetLedgerSummaryDbEntity;
import com.eprocure.finance.infrastructure.persistence.entity.BudgetOverrideApprovalDbEntity;
import com.eprocure.finance.infrastructure.persistence.entity.BudgetTransactionDbEntity;
import com.eprocure.finance.infrastructure.persistence.entity.BudgetTransferDbEntity;
import java.math.BigDecimal;
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
public interface BudgetMapper {

    @Select("""
            SELECT
                b.id,
                b.department_id,
                b.fiscal_year,
                b.quarter,
                b.gl_account_code,
                b.allocated_amount,
                b.currency,
                b.status,
                COALESCE(SUM(CASE
                    WHEN t.transaction_type IN ('COMMIT_TENTATIVE', 'COMMIT_FIRM') THEN t.amount
                    WHEN t.transaction_type = 'RELEASE' THEN -t.amount
                    ELSE 0
                END), 0) AS committed_amount,
                COALESCE(SUM(CASE
                    WHEN t.transaction_type = 'SPEND' THEN t.amount
                    ELSE 0
                END), 0) AS spent_amount
            FROM finance.budgets b
            LEFT JOIN finance.budget_transactions t ON t.budget_id = b.id
            WHERE b.department_id = #{departmentId}
              AND b.fiscal_year = #{fiscalYear}
              AND b.status = 'ACTIVE'
              AND b.is_deleted = FALSE
              AND (#{glAccountCode,jdbcType=VARCHAR} IS NULL OR b.gl_account_code = #{glAccountCode,jdbcType=VARCHAR})
            GROUP BY b.id, b.department_id, b.fiscal_year, b.quarter, b.gl_account_code,
                     b.allocated_amount, b.currency, b.status
            ORDER BY
                CASE WHEN b.quarter IS NULL THEN 1 ELSE 0 END,
                b.quarter DESC,
                b.gl_account_code ASC
            LIMIT 1
            """)
    @Results(id = "budgetLedgerSummaryResult", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "departmentId", column = "department_id"),
            @Result(property = "fiscalYear", column = "fiscal_year"),
            @Result(property = "quarter", column = "quarter"),
            @Result(property = "glAccountCode", column = "gl_account_code"),
            @Result(property = "allocatedAmount", column = "allocated_amount"),
            @Result(property = "committedAmount", column = "committed_amount"),
            @Result(property = "spentAmount", column = "spent_amount"),
            @Result(property = "currency", column = "currency"),
            @Result(property = "status", column = "status")
    })
    Optional<BudgetLedgerSummaryDbEntity> findActiveSummary(
            @Param("departmentId") UUID departmentId,
            @Param("fiscalYear") int fiscalYear,
            @Param("glAccountCode") String glAccountCode);

    List<BudgetLedgerSummaryDbEntity> findByFilter(@Param("filter") BudgetFilter filter);

    long countByFilter(@Param("filter") BudgetFilter filter);

    Optional<BudgetLedgerSummaryDbEntity> findSummaryById(@Param("budgetId") UUID budgetId);

    @Select("""
            SELECT id
            FROM finance.budgets
            WHERE id = #{budgetId}
              AND is_deleted = FALSE
            FOR UPDATE
            """)
    Optional<UUID> lockBudgetForUpdate(@Param("budgetId") UUID budgetId);

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
            SELECT EXISTS (
                SELECT 1
                FROM finance.budget_transactions
                WHERE budget_id = #{budgetId}
                  AND transaction_type = #{transactionType}
                  AND reference_type = #{referenceType}
                  AND reference_id = #{referenceId}
            )
            """)
    boolean existsTransaction(
            @Param("budgetId") UUID budgetId,
            @Param("transactionType") BudgetTransactionType transactionType,
            @Param("referenceType") String referenceType,
            @Param("referenceId") UUID referenceId);

    @Insert("""
            INSERT INTO finance.budget_transactions (
                budget_id, transaction_type, amount, currency, reference_type, reference_id,
                description, performed_by, performed_at, source_event_id
            ) VALUES (
                #{entity.budgetId},
                #{entity.transactionType},
                #{entity.amount},
                #{entity.currency},
                #{entity.referenceType},
                #{entity.referenceId},
                #{entity.description},
                #{entity.performedBy},
                #{entity.performedAt},
                #{entity.sourceEventId}
            )
            """)
    void insertTransaction(@Param("entity") BudgetTransactionDbEntity entity);

    @Select("""
            SELECT
                budget_id,
                currency,
                SUM(CASE
                    WHEN transaction_type IN ('COMMIT_TENTATIVE', 'COMMIT_FIRM') THEN amount
                    WHEN transaction_type = 'RELEASE' THEN -amount
                    ELSE 0
                END) AS amount
            FROM finance.budget_transactions
            WHERE reference_type = #{referenceType}
              AND reference_id = #{referenceId}
            GROUP BY budget_id, currency
            HAVING SUM(CASE
                    WHEN transaction_type IN ('COMMIT_TENTATIVE', 'COMMIT_FIRM') THEN amount
                    WHEN transaction_type = 'RELEASE' THEN -amount
                    ELSE 0
                END) > 0
            ORDER BY MAX(performed_at) DESC
            LIMIT 1
            """)
    @Results(id = "budgetCommitmentHoldResult", value = {
            @Result(property = "budgetId", column = "budget_id"),
            @Result(property = "heldAmount", column = "amount"),
            @Result(property = "currency", column = "currency")
    })
    Optional<BudgetCommitmentHoldDbEntity> findHeldCommitment(
            @Param("referenceType") String referenceType,
            @Param("referenceId") UUID referenceId);

    @Select("""
            SELECT
                id,
                budget_id,
                purchase_request_id,
                override_amount AS amount,
                currency,
                override_reason AS reason,
                approved_by,
                approved_at,
                idempotency_key,
                status
            FROM finance.budget_overrides
            WHERE idempotency_key = #{idempotencyKey}
              AND is_deleted = FALSE
            """)
    @Results(id = "budgetOverrideApprovalResult", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "budgetId", column = "budget_id"),
            @Result(property = "purchaseRequestId", column = "purchase_request_id"),
            @Result(property = "amount", column = "amount"),
            @Result(property = "currency", column = "currency"),
            @Result(property = "reason", column = "reason"),
            @Result(property = "approvedBy", column = "approved_by"),
            @Result(property = "approvedAt", column = "approved_at"),
            @Result(property = "idempotencyKey", column = "idempotency_key"),
            @Result(property = "status", column = "status")
    })
    Optional<BudgetOverrideApprovalDbEntity> findOverrideApprovalByIdempotencyKey(
            @Param("idempotencyKey") UUID idempotencyKey);

    @Insert("""
            INSERT INTO finance.budget_overrides (
                id, budget_id, purchase_request_id, override_amount, currency, override_reason,
                status, approved_by, approved_at, idempotency_key, created_by
            ) VALUES (
                #{entity.id},
                #{entity.budgetId},
                #{entity.purchaseRequestId},
                #{entity.amount},
                #{entity.currency},
                #{entity.reason},
                #{entity.status},
                #{entity.approvedBy},
                #{entity.approvedAt},
                #{entity.idempotencyKey},
                #{entity.approvedBy}
            )
            """)
    void insertOverrideApproval(@Param("entity") BudgetOverrideApprovalDbEntity entity);

    @Select("""
            SELECT
                id,
                source_budget_id,
                target_budget_id,
                amount,
                currency,
                reason,
                approved_by,
                approved_at,
                idempotency_key
            FROM finance.budget_transfers
            WHERE idempotency_key = #{idempotencyKey}
              AND is_deleted = FALSE
            """)
    @Results(id = "budgetTransferResult", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "sourceBudgetId", column = "source_budget_id"),
            @Result(property = "targetBudgetId", column = "target_budget_id"),
            @Result(property = "amount", column = "amount"),
            @Result(property = "currency", column = "currency"),
            @Result(property = "reason", column = "reason"),
            @Result(property = "approvedBy", column = "approved_by"),
            @Result(property = "approvedAt", column = "approved_at"),
            @Result(property = "idempotencyKey", column = "idempotency_key")
    })
    Optional<BudgetTransferDbEntity> findTransferByIdempotencyKey(@Param("idempotencyKey") UUID idempotencyKey);

    @Insert("""
            INSERT INTO finance.budget_transfers (
                id, source_budget_id, target_budget_id, amount, currency, reason,
                approved_by, approved_at, idempotency_key, created_by
            ) VALUES (
                #{entity.id},
                #{entity.sourceBudgetId},
                #{entity.targetBudgetId},
                #{entity.amount},
                #{entity.currency},
                #{entity.reason},
                #{entity.approvedBy},
                #{entity.approvedAt},
                #{entity.idempotencyKey},
                #{entity.approvedBy}
            )
            """)
    void insertTransfer(@Param("entity") BudgetTransferDbEntity entity);

    @Update("""
            UPDATE finance.budgets
            SET allocated_amount = allocated_amount + #{delta},
                updated_by = #{actorId}
            WHERE id = #{budgetId}
              AND is_deleted = FALSE
            """)
    int adjustAllocatedAmount(
            @Param("budgetId") UUID budgetId,
            @Param("delta") BigDecimal delta,
            @Param("actorId") UUID actorId);
}
