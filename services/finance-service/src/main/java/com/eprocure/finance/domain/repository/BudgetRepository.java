package com.eprocure.finance.domain.repository;

import com.eprocure.finance.domain.model.BudgetCheckCriteria;
import com.eprocure.finance.domain.model.BudgetCommitmentHold;
import com.eprocure.finance.domain.model.BudgetLedgerSummary;
import com.eprocure.finance.domain.model.BudgetTransaction;
import com.eprocure.finance.domain.model.BudgetTransactionType;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository {
    Optional<BudgetLedgerSummary> findActiveSummary(BudgetCheckCriteria criteria);

    boolean existsProcessedEvent(String eventId);

    void markEventProcessed(String eventId, String topic, Integer partitionId, Long offsetValue, String handlerName);

    boolean existsTransaction(UUID budgetId, BudgetTransactionType transactionType, String referenceType, UUID referenceId);

    void insertTransaction(BudgetTransaction transaction);

    Optional<BudgetCommitmentHold> findHeldCommitment(String referenceType, UUID referenceId);
}
