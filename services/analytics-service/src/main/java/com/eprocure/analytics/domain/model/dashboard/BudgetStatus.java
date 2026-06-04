package com.eprocure.analytics.domain.model.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BudgetStatus(
        BigDecimal allocated,
        BigDecimal committed,
        BigDecimal spent,
        BigDecimal available,
        BigDecimal availablePct,
        LocalDate forecastRunOutDate) {

    public BudgetStatus {
        allocated = zeroIfNull(allocated);
        committed = zeroIfNull(committed);
        spent = zeroIfNull(spent);
        available = zeroIfNull(available);
        availablePct = zeroIfNull(availablePct);
    }

    public static BudgetStatus empty() {
        return new BudgetStatus(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null);
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
