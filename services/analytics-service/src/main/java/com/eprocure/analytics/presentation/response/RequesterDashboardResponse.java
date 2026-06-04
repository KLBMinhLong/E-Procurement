package com.eprocure.analytics.presentation.response;

import java.util.List;

public record RequesterDashboardResponse(
        MyPurchaseRequestStatsResponse myPrStats,
        DepartmentBudgetSummaryResponse departmentBudget,
        List<RequesterRecentPrResponse> recentPrs) {
}
