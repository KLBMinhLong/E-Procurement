package com.eprocure.analytics.domain.model.dashboard;

import com.eprocure.analytics.domain.model.KpiCard;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ManagerDashboard(
        List<KpiCard> kpis,
        BudgetStatus budgetStatus,
        PendingApprovals pendingApprovals,
        List<RecentPurchaseRequest> recentPrs,
        List<SlaWarning> slaWarnings,
        Instant cachedAt) {

    public ManagerDashboard {
        kpis = List.copyOf(kpis == null ? List.of() : kpis);
        budgetStatus = budgetStatus == null ? BudgetStatus.empty() : budgetStatus;
        pendingApprovals = pendingApprovals == null ? PendingApprovals.empty() : pendingApprovals;
        recentPrs = List.copyOf(recentPrs == null ? List.of() : recentPrs);
        slaWarnings = List.copyOf(slaWarnings == null ? List.of() : slaWarnings);
        cachedAt = Objects.requireNonNull(cachedAt, "cachedAt must not be null");
    }

    public static ManagerDashboard empty(Instant cachedAt) {
        return new ManagerDashboard(List.of(), BudgetStatus.empty(), PendingApprovals.empty(), List.of(), List.of(), cachedAt);
    }
}
