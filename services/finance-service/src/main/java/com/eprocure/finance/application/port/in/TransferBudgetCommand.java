package com.eprocure.finance.application.port.in;

import com.eprocure.finance.domain.model.vo.Money;
import java.util.Objects;
import java.util.UUID;

public record TransferBudgetCommand(
        UUID actorId,
        UUID sourceBudgetId,
        UUID targetBudgetId,
        Money amount,
        String reason) {

    public TransferBudgetCommand {
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        sourceBudgetId = Objects.requireNonNull(sourceBudgetId, "sourceBudgetId must not be null");
        targetBudgetId = Objects.requireNonNull(targetBudgetId, "targetBudgetId must not be null");
        amount = Objects.requireNonNull(amount, "amount must not be null");
        reason = reason == null ? null : reason.trim();
    }
}
