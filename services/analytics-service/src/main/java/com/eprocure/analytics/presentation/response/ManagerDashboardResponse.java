package com.eprocure.analytics.presentation.response;

import java.util.List;

public record ManagerDashboardResponse(
        List<KpiCardResponse> kpis,
        BudgetStatusResponse budgetStatus,
        PendingApprovalsResponse pendingApprovals,
        List<ManagerRecentPrResponse> recentPrs,
        List<SlaWarningResponse> slaWarnings) {
}
