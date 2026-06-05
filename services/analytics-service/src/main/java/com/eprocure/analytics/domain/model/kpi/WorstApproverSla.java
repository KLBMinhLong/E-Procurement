package com.eprocure.analytics.domain.model.kpi;

import java.math.BigDecimal;

public record WorstApproverSla(
        String approverName,
        int overdueCount,
        BigDecimal compliancePct) {

    public WorstApproverSla {
        approverName = approverName == null || approverName.isBlank() ? "UNKNOWN" : approverName.trim();
        overdueCount = Math.max(overdueCount, 0);
        compliancePct = compliancePct == null ? BigDecimal.ZERO : compliancePct;
    }
}
