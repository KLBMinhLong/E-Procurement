package com.eprocure.finance.infrastructure.persistence.entity;

import com.eprocure.finance.domain.model.BudgetTransaction;
import com.eprocure.finance.domain.model.BudgetTransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class BudgetTransactionDbEntity {
    private UUID budgetId;
    private BudgetTransactionType transactionType;
    private BigDecimal amount;
    private String currency;
    private String referenceType;
    private UUID referenceId;
    private String description;
    private UUID performedBy;
    private Instant performedAt;
    private String sourceEventId;

    public static BudgetTransactionDbEntity from(BudgetTransaction transaction) {
        BudgetTransactionDbEntity entity = new BudgetTransactionDbEntity();
        entity.budgetId = transaction.budgetId();
        entity.transactionType = transaction.transactionType();
        entity.amount = transaction.money().amount();
        entity.currency = transaction.money().currency();
        entity.referenceType = transaction.referenceType();
        entity.referenceId = transaction.referenceId();
        entity.description = transaction.description();
        entity.performedBy = transaction.performedBy();
        entity.performedAt = transaction.performedAt();
        entity.sourceEventId = transaction.sourceEventId();
        return entity;
    }

    public UUID getBudgetId() {
        return budgetId;
    }

    public BudgetTransactionType getTransactionType() {
        return transactionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public String getDescription() {
        return description;
    }

    public UUID getPerformedBy() {
        return performedBy;
    }

    public Instant getPerformedAt() {
        return performedAt;
    }

    public String getSourceEventId() {
        return sourceEventId;
    }
}
