package com.eprocure.analytics.domain.model.dashboard;

import java.math.BigDecimal;

public record DepartmentBudgetSummary(
        BigDecimal available,
        BigDecimal availablePct) {

    public DepartmentBudgetSummary {
        available = available == null ? BigDecimal.ZERO : available;
        availablePct = availablePct == null ? BigDecimal.ZERO : availablePct;
    }

    public static DepartmentBudgetSummary empty() {
        return new DepartmentBudgetSummary(BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
