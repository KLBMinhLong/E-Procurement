package com.eprocure.finance.infrastructure.persistence.repository;

import com.eprocure.finance.domain.model.BudgetCheckCriteria;
import com.eprocure.finance.domain.model.BudgetCommitmentHold;
import com.eprocure.finance.domain.model.BudgetLedgerSummary;
import com.eprocure.finance.domain.model.BudgetTransaction;
import com.eprocure.finance.domain.model.BudgetTransactionType;
import com.eprocure.finance.domain.repository.BudgetRepository;
import com.eprocure.finance.infrastructure.persistence.entity.BudgetTransactionDbEntity;
import com.eprocure.finance.infrastructure.persistence.mapper.BudgetMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class BudgetRepositoryImpl implements BudgetRepository {
    private final BudgetMapper budgetMapper;
    private final ObjectMapper objectMapper;

    public BudgetRepositoryImpl(
            BudgetMapper budgetMapper,
            @Qualifier("domainObjectMapper") ObjectMapper objectMapper) {
        this.budgetMapper = budgetMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<BudgetLedgerSummary> findActiveSummary(BudgetCheckCriteria criteria) {
        return budgetMapper.findActiveSummary(
                        criteria.departmentId(),
                        criteria.fiscalYear(),
                        criteria.glAccountCode())
                .map(entity -> objectMapper.convertValue(entity, BudgetLedgerSummary.class));
    }

    @Override
    public boolean existsProcessedEvent(String eventId) {
        return budgetMapper.existsProcessedEvent(eventId);
    }

    @Override
    public void markEventProcessed(String eventId, String topic, Integer partitionId, Long offsetValue, String handlerName) {
        budgetMapper.markEventProcessed(eventId, topic, partitionId, offsetValue, handlerName);
    }

    @Override
    public boolean existsTransaction(UUID budgetId, BudgetTransactionType transactionType, String referenceType, UUID referenceId) {
        return budgetMapper.existsTransaction(budgetId, transactionType, referenceType, referenceId);
    }

    @Override
    public void insertTransaction(BudgetTransaction transaction) {
        budgetMapper.insertTransaction(BudgetTransactionDbEntity.from(transaction));
    }

    @Override
    public Optional<BudgetCommitmentHold> findHeldCommitment(String referenceType, UUID referenceId) {
        return budgetMapper.findHeldCommitment(referenceType, referenceId)
                .map(entity -> objectMapper.convertValue(entity, BudgetCommitmentHold.class));
    }
}
