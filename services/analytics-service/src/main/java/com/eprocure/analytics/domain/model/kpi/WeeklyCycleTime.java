package com.eprocure.analytics.domain.model.kpi;

import java.math.BigDecimal;

public record WeeklyCycleTime(
        String week,
        BigDecimal avgHours) {

    public WeeklyCycleTime {
        week = week == null || week.isBlank() ? "UNKNOWN" : week.trim();
        avgHours = avgHours == null ? BigDecimal.ZERO : avgHours;
    }
}
