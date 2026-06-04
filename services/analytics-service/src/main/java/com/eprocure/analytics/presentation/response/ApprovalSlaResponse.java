package com.eprocure.analytics.presentation.response;

import java.math.BigDecimal;

public record ApprovalSlaResponse(
        BigDecimal onTimePercent,
        BigDecimal avgCycleHours,
        int overdueCount) {
}
