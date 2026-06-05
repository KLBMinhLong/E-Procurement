package com.eprocure.analytics.domain.model.kpi;

import java.math.BigDecimal;

public record ApproverRoleSla(
        String role,
        BigDecimal compliancePct,
        BigDecimal avgActionHours,
        int overdueCount) {

    public ApproverRoleSla {
        role = role == null || role.isBlank() ? "UNKNOWN" : role.trim();
        compliancePct = compliancePct == null ? BigDecimal.ZERO : compliancePct;
        avgActionHours = avgActionHours == null ? BigDecimal.ZERO : avgActionHours;
        overdueCount = Math.max(overdueCount, 0);
    }
}
