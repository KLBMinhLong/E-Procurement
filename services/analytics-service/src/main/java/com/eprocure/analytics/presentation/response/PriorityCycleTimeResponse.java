package com.eprocure.analytics.presentation.response;

import java.math.BigDecimal;

public record PriorityCycleTimeResponse(
        String priority,
        BigDecimal avgHours) {
}
