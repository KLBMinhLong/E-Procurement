package com.eprocure.finance.application.port.in;

import com.eprocure.finance.domain.model.vo.Money;
import java.util.Objects;
import java.util.UUID;

public record ApproveBudgetOverrideCommand(
        UUID actorId,
        UUID budgetId,
        UUID purchaseRequestId,
        Money overrideAmount,
        String overrideReason) {

    public ApproveBudgetOverrideCommand {
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        budgetId = Objects.requireNonNull(budgetId, "budgetId must not be null");
        purchaseRequestId = Objects.requireNonNull(purchaseRequestId, "purchaseRequestId must not be null");
        overrideAmount = Objects.requireNonNull(overrideAmount, "overrideAmount must not be null");
        overrideReason = overrideReason == null ? null : overrideReason.trim();
    }
}
