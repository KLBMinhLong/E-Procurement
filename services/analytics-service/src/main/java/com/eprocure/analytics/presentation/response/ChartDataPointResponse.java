package com.eprocure.analytics.presentation.response;

import java.math.BigDecimal;

public record ChartDataPointResponse(
        String label,
        BigDecimal value,
        BigDecimal value2) {
}
