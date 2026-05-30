package com.eprocure.finance.infrastructure.persistence.repository;

import com.eprocure.finance.domain.model.BudgetCheckCriteria;
import com.eprocure.finance.domain.model.BudgetCommitmentHold;
import com.eprocure.finance.domain.model.BudgetLedgerSummary;
import com.eprocure.finance.domain.model.BudgetOverrideApproval;
import com.eprocure.finance.domain.model.BudgetTransaction;
import com.eprocure.finance.domain.model.BudgetTransactionType;
import com.eprocure.finance.domain.model.BudgetTransfer;
import com.eprocure.finance.domain.model.vo.Money;
import com.eprocure.finance.infrastructure.persistence.entity.BudgetOverrideApprovalDbEntity;
import com.eprocure.finance.domain.repository.BudgetFilter;
import com.eprocure.finance.domain.repository.BudgetRepository;
import com.eprocure.finance.infrastructure.persistence.entity.BudgetTransactionDbEntity;
import com.eprocure.finance.infrastructure.persistence.entity.BudgetTransferDbEntity;
import com.eprocure.finance.infrastructure.persistence.mapper.BudgetMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
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
    public List<BudgetLedgerSummary> findByFilter(BudgetFilter filter) {
        return budgetMapper.findByFilter(filter).stream()
                .map(entity -> objectMapper.convertValue(entity, BudgetLedgerSummary.class))
                .toList();
    }

    @Override
    public long countByFilter(BudgetFilter filter) {
        return budgetMapper.countByFilter(filter);
    }

    @Override
    public Optional<BudgetLedgerSummary> findSummaryById(UUID budgetId) {
        return budgetMapper.findSummaryById(budgetId)
                .map(entity -> objectMapper.convertValue(entity, BudgetLedgerSummary.class));
    }

    @Override
    public boolean lockBudgetForUpdate(UUID budgetId) {
        return budgetMapper.lockBudgetForUpdate(budgetId).isPresent();
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

    @Override
    public Optional<BudgetOverrideApproval> findOverrideApprovalByIdempotencyKey(UUID idempotencyKey) {
        return budgetMapper.findOverrideApprovalByIdempotencyKey(idempotencyKey)
                .map(entity -> objectMapper.convertValue(entity, BudgetOverrideApproval.class));
    }

    @Override
    public void insertOverrideApproval(BudgetOverrideApproval approval) {
        budgetMapper.insertOverrideApproval(BudgetOverrideApprovalDbEntity.from(approval));
    }

    @Override
    public Optional<BudgetTransfer> findTransferByIdempotencyKey(UUID idempotencyKey) {
        return budgetMapper.findTransferByIdempotencyKey(idempotencyKey)
                .map(entity -> objectMapper.convertValue(entity, BudgetTransfer.class));
    }

    @Override
    public void insertTransfer(BudgetTransfer transfer) {
        budgetMapper.insertTransfer(BudgetTransferDbEntity.from(transfer));
    }

    @Override
    public void adjustAllocatedAmount(UUID budgetId, Money delta, UUID actorId) {
        budgetMapper.adjustAllocatedAmount(budgetId, delta.amount(), actorId);
    }
}
