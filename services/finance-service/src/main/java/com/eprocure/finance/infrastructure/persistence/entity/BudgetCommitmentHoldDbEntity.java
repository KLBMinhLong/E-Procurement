package com.eprocure.finance.infrastructure.persistence.entity;

import com.eprocure.finance.domain.model.vo.Money;
import java.math.BigDecimal;
import java.util.UUID;

public class BudgetCommitmentHoldDbEntity {
    private UUID budgetId;
    private BigDecimal heldAmount;
    private String currency;

    public UUID getBudgetId() {
        return budgetId;
    }

    public void setBudgetId(UUID budgetId) {
        this.budgetId = budgetId;
    }

    public void setHeldAmount(BigDecimal heldAmount) {
        this.heldAmount = heldAmount;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Money getAmount() {
        return new Money(heldAmount, currency);
    }
}
