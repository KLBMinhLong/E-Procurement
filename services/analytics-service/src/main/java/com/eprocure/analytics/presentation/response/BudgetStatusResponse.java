package com.eprocure.analytics.presentation.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BudgetStatusResponse(
        String allocated,
        String committed,
        String spent,
        String available,
        BigDecimal availablePct,
        LocalDate forecastRunOutDate) {
}
