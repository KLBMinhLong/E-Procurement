package com.eprocure.analytics.presentation.response;

import java.util.List;

public record PurchasingDashboardResponse(
        List<KpiCardResponse> kpis,
        PoPipelineResponse poPipeline,
        int openRfqs,
        int grPending,
        int invoicesPendingMatch,
        List<VendorPerformanceResponse> vendorPerformance) {
}
