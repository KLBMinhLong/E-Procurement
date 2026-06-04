package com.eprocure.analytics.presentation.response;

import java.math.BigDecimal;

public record DepartmentBudgetSummaryResponse(
        String available,
        BigDecimal availablePct) {
}
