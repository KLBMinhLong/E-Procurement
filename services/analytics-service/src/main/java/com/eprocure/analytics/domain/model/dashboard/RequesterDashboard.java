package com.eprocure.analytics.domain.model.dashboard;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record RequesterDashboard(
        MyPurchaseRequestStats myPrStats,
        DepartmentBudgetSummary departmentBudget,
        List<RecentPurchaseRequest> recentPrs,
        Instant cachedAt) {

    public RequesterDashboard {
        myPrStats = myPrStats == null ? MyPurchaseRequestStats.empty() : myPrStats;
        departmentBudget = departmentBudget == null ? DepartmentBudgetSummary.empty() : departmentBudget;
        recentPrs = List.copyOf(recentPrs == null ? List.of() : recentPrs);
        cachedAt = Objects.requireNonNull(cachedAt, "cachedAt must not be null");
    }

    public static RequesterDashboard empty(Instant cachedAt) {
        return new RequesterDashboard(MyPurchaseRequestStats.empty(), DepartmentBudgetSummary.empty(), List.of(), cachedAt);
    }
}
