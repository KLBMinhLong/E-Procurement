package com.eprocure.finance.domain.model;

import com.eprocure.finance.domain.model.vo.Money;
import java.util.Objects;
import java.util.UUID;

public record BudgetCommitmentHold(
        UUID budgetId,
        Money amount) {

    public BudgetCommitmentHold {
        budgetId = Objects.requireNonNull(budgetId, "budgetId must not be null");
        amount = Objects.requireNonNull(amount, "amount must not be null");
    }

    public boolean hasHeldAmount() {
        return amount.isPositive();
    }
}
