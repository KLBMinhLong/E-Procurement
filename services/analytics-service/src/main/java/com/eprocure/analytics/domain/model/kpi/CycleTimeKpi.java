package com.eprocure.analytics.domain.model.kpi;

import java.math.BigDecimal;
import java.util.List;

public record CycleTimeKpi(
        BigDecimal avgCycleHours,
        BigDecimal medianCycleHours,
        BigDecimal p95CycleHours,
        BigDecimal target,
        List<PriorityCycleTime> byPriority,
        List<WeeklyCycleTime> trend) {

    public CycleTimeKpi {
        avgCycleHours = normalize(avgCycleHours);
        medianCycleHours = normalize(medianCycleHours);
        p95CycleHours = normalize(p95CycleHours);
        target = normalize(target);
        byPriority = List.copyOf(byPriority == null ? List.of() : byPriority);
        trend = List.copyOf(trend == null ? List.of() : trend);
    }

    public static CycleTimeKpi foundation(BigDecimal target) {
        return new CycleTimeKpi(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, target, List.of(), List.of());
    }

    private static BigDecimal normalize(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
