package com.eprocure.analytics.domain.model.dashboard;

import com.eprocure.analytics.domain.model.KpiCard;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record PurchasingDashboard(
        List<KpiCard> kpis,
        PoPipeline poPipeline,
        int openRfqs,
        int grPending,
        int invoicesPendingMatch,
        List<VendorPerformance> vendorPerformance,
        Instant cachedAt) {

    public PurchasingDashboard {
        kpis = List.copyOf(kpis == null ? List.of() : kpis);
        poPipeline = poPipeline == null ? PoPipeline.empty() : poPipeline;
        openRfqs = Math.max(openRfqs, 0);
        grPending = Math.max(grPending, 0);
        invoicesPendingMatch = Math.max(invoicesPendingMatch, 0);
        vendorPerformance = List.copyOf(vendorPerformance == null ? List.of() : vendorPerformance);
        cachedAt = Objects.requireNonNull(cachedAt, "cachedAt must not be null");
    }

    public static PurchasingDashboard empty(Instant cachedAt) {
        return new PurchasingDashboard(List.of(), PoPipeline.empty(), 0, 0, 0, List.of(), cachedAt);
    }
}
