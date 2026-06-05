package com.eprocure.analytics.presentation.response;

import java.math.BigDecimal;
import java.util.List;

public record CycleTimeKpiResponse(
        BigDecimal avgCycleHours,
        BigDecimal medianCycleHours,
        BigDecimal p95CycleHours,
        BigDecimal target,
        List<PriorityCycleTimeResponse> byPriority,
        List<WeeklyCycleTimeResponse> trend) {
}
