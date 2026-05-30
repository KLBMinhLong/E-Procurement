package com.eprocure.finance.domain.model;

import com.eprocure.finance.domain.model.vo.Money;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record BudgetOverrideApproval(
        UUID id,
        UUID budgetId,
        UUID purchaseRequestId,
        Money money,
        String reason,
        UUID approvedBy,
        Instant approvedAt,
        UUID idempotencyKey,
        BudgetOverrideStatus status) {

    public BudgetOverrideApproval {
        id = Objects.requireNonNull(id, "id must not be null");
        budgetId = Objects.requireNonNull(budgetId, "budgetId must not be null");
        purchaseRequestId = Objects.requireNonNull(purchaseRequestId, "purchaseRequestId must not be null");
        money = Objects.requireNonNull(money, "money must not be null");
        if (!money.isPositive()) {
            throw new IllegalArgumentException("override amount must be positive");
        }
        reason = requireText(reason, "reason");
        approvedBy = Objects.requireNonNull(approvedBy, "approvedBy must not be null");
        approvedAt = Objects.requireNonNull(approvedAt, "approvedAt must not be null");
        idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        status = Objects.requireNonNull(status, "status must not be null");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
