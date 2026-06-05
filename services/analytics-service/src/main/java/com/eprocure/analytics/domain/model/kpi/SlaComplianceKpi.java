package com.eprocure.analytics.domain.model.kpi;

import java.math.BigDecimal;
import java.util.List;

public record SlaComplianceKpi(
        BigDecimal overallCompliancePct,
        List<ApproverRoleSla> byApproverRole,
        List<WorstApproverSla> worstApprovers) {

    public SlaComplianceKpi {
        overallCompliancePct = overallCompliancePct == null ? BigDecimal.ZERO : overallCompliancePct;
        byApproverRole = List.copyOf(byApproverRole == null ? List.of() : byApproverRole);
        worstApprovers = List.copyOf(worstApprovers == null ? List.of() : worstApprovers);
    }

    public static SlaComplianceKpi empty() {
        return new SlaComplianceKpi(BigDecimal.ZERO, List.of(), List.of());
    }
}
