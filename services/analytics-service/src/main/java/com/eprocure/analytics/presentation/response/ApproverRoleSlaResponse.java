package com.eprocure.analytics.presentation.response;

import java.math.BigDecimal;

public record ApproverRoleSlaResponse(
        String role,
        BigDecimal compliancePct,
        BigDecimal avgActionHours,
        int overdueCount) {
}
