package com.eprocure.analytics.presentation.response;

import java.time.Instant;
import java.util.List;

public record ExecutiveDashboardResponse(
        List<KpiCardResponse> kpis,
        List<DepartmentSpendResponse> spendByDepartment,
        List<ChartDataPointResponse> spendByCategory,
        List<MonthlyTrendResponse> monthlyTrend,
        ApprovalSlaResponse approvalSla,
        List<TopVendorResponse> topVendors,
        Instant cachedAt) {
}
