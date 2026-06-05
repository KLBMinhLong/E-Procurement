package com.eprocure.analytics.presentation.response;

import java.math.BigDecimal;

public record WorstApproverSlaResponse(
        String approverName,
        int overdueCount,
        BigDecimal compliancePct) {
}
