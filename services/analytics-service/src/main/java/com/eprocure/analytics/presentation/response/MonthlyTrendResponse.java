package com.eprocure.analytics.presentation.response;

import java.math.BigDecimal;

public record MonthlyTrendResponse(
        String month,
        BigDecimal spent,
        BigDecimal budget,
        int prCount) {
}
