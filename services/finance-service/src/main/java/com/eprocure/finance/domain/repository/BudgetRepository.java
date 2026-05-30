package com.eprocure.finance.domain.repository;

import com.eprocure.finance.domain.model.BudgetCheckCriteria;
import com.eprocure.finance.domain.model.BudgetCommitmentHold;
import com.eprocure.finance.domain.model.BudgetLedgerSummary;
import com.eprocure.finance.domain.model.BudgetOverrideApproval;
import com.eprocure.finance.domain.model.BudgetTransaction;
import com.eprocure.finance.domain.model.BudgetTransactionType;
import com.eprocure.finance.domain.model.BudgetTransfer;
import com.eprocure.finance.domain.model.vo.Money;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository {
    Optional<BudgetLedgerSummary> findActiveSummary(BudgetCheckCriteria criteria);

    List<BudgetLedgerSummary> findByFilter(BudgetFilter filter);

    long countByFilter(BudgetFilter filter);

    Optional<BudgetLedgerSummary> findSummaryById(UUID budgetId);

    boolean lockBudgetForUpdate(UUID budgetId);

    boolean existsProcessedEvent(String eventId);

    void markEventProcessed(String eventId, String topic, Integer partitionId, Long offsetValue, String handlerName);

    boolean existsTransaction(UUID budgetId, BudgetTransactionType transactionType, String referenceType, UUID referenceId);

    void insertTransaction(BudgetTransaction transaction);

    Optional<BudgetCommitmentHold> findHeldCommitment(String referenceType, UUID referenceId);

    Optional<BudgetOverrideApproval> findOverrideApprovalByIdempotencyKey(UUID idempotencyKey);

    void insertOverrideApproval(BudgetOverrideApproval approval);

    Optional<BudgetTransfer> findTransferByIdempotencyKey(UUID idempotencyKey);

    void insertTransfer(BudgetTransfer transfer);

    void adjustAllocatedAmount(UUID budgetId, Money delta, UUID actorId);
}
