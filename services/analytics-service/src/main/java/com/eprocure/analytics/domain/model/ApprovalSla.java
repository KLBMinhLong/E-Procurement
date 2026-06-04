package com.eprocure.analytics.domain.model;

import java.math.BigDecimal;

public record ApprovalSla(
        BigDecimal onTimePercent,
        BigDecimal avgCycleHours,
        int overdueCount) {

    public ApprovalSla {
        onTimePercent = onTimePercent == null ? BigDecimal.ZERO : onTimePercent;
        avgCycleHours = avgCycleHours == null ? BigDecimal.ZERO : avgCycleHours;
        if (overdueCount < 0) {
            throw new IllegalArgumentException("overdueCount must not be negative");
        }
    }
}
