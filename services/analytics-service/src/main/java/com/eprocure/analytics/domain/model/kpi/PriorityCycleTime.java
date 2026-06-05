package com.eprocure.analytics.domain.model.kpi;

import java.math.BigDecimal;

public record PriorityCycleTime(
        String priority,
        BigDecimal avgHours) {

    public PriorityCycleTime {
        priority = priority == null || priority.isBlank() ? "UNKNOWN" : priority.trim();
        avgHours = avgHours == null ? BigDecimal.ZERO : avgHours;
    }
}
