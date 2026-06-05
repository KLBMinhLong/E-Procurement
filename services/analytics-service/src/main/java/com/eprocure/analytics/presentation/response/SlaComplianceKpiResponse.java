package com.eprocure.analytics.presentation.response;

import java.math.BigDecimal;
import java.util.List;

public record SlaComplianceKpiResponse(
        BigDecimal overallCompliancePct,
        List<ApproverRoleSlaResponse> byApproverRole,
        List<WorstApproverSlaResponse> worstApprovers) {
}
