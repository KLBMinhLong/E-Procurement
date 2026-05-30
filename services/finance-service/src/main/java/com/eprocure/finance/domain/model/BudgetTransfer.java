package com.eprocure.finance.domain.model;

import com.eprocure.finance.domain.model.vo.Money;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record BudgetTransfer(
        UUID id,
        UUID sourceBudgetId,
        UUID targetBudgetId,
        Money money,
        String reason,
        UUID approvedBy,
        Instant approvedAt,
        UUID idempotencyKey) {

    public BudgetTransfer {
        id = Objects.requireNonNull(id, "id must not be null");
        sourceBudgetId = Objects.requireNonNull(sourceBudgetId, "sourceBudgetId must not be null");
        targetBudgetId = Objects.requireNonNull(targetBudgetId, "targetBudgetId must not be null");
        if (sourceBudgetId.equals(targetBudgetId)) {
            throw new IllegalArgumentException("source and target budget must be different");
        }
        money = Objects.requireNonNull(money, "money must not be null");
        if (!money.isPositive()) {
            throw new IllegalArgumentException("transfer amount must be positive");
        }
        reason = requireText(reason, "reason");
        approvedBy = Objects.requireNonNull(approvedBy, "approvedBy must not be null");
        approvedAt = Objects.requireNonNull(approvedAt, "approvedAt must not be null");
        idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
