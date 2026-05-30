package com.eprocure.finance.domain.model;

import com.eprocure.finance.domain.model.vo.Money;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record BudgetTransaction(
        UUID budgetId,
        BudgetTransactionType transactionType,
        Money money,
        String referenceType,
        UUID referenceId,
        String description,
        UUID performedBy,
        Instant performedAt,
        String sourceEventId) {

    public BudgetTransaction {
        budgetId = Objects.requireNonNull(budgetId, "budgetId must not be null");
        transactionType = Objects.requireNonNull(transactionType, "transactionType must not be null");
        money = Objects.requireNonNull(money, "money must not be null");
        if (!money.isPositive()) {
            throw new IllegalArgumentException("transaction amount must be positive");
        }
        referenceType = requireText(referenceType, "referenceType");
        referenceId = Objects.requireNonNull(referenceId, "referenceId must not be null");
        description = description == null ? null : description.trim();
        performedBy = Objects.requireNonNull(performedBy, "performedBy must not be null");
        performedAt = Objects.requireNonNull(performedAt, "performedAt must not be null");
        sourceEventId = sourceEventId == null || sourceEventId.isBlank() ? null : sourceEventId.trim();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
