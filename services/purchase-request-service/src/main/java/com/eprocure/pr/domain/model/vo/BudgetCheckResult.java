package com.eprocure.pr.domain.model.vo;

import java.util.Objects;

public record BudgetCheckResult(
        Money allocated,
        Money committed,
        Money spent,
        Money available,
        BudgetCheckStatus status,
        String warningMessage) {

    public BudgetCheckResult {
        allocated = Objects.requireNonNull(allocated, "allocated must not be null");
        committed = Objects.requireNonNull(committed, "committed must not be null");
        spent = Objects.requireNonNull(spent, "spent must not be null");
        available = Objects.requireNonNull(available, "available must not be null");
        status = Objects.requireNonNull(status, "status must not be null");
        warningMessage = normalizeOptionalText(warningMessage);
    }

    public static BudgetCheckResult pass(Money available) {
        Money zero = Money.zero(available.currency());
        return new BudgetCheckResult(available, zero, zero, available, BudgetCheckStatus.PASS, null);
    }

    public boolean requiresOverride() {
        return status == BudgetCheckStatus.FAIL;
    }

    private static String normalizeOptionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
