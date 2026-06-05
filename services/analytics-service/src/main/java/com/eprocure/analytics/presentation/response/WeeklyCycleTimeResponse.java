package com.eprocure.analytics.presentation.response;

import java.math.BigDecimal;

public record WeeklyCycleTimeResponse(
        String week,
        BigDecimal avgHours) {
}
