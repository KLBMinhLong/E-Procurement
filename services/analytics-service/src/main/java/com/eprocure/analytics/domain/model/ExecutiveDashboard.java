package com.eprocure.analytics.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record ExecutiveDashboard(
        UUID id,
        int fiscalYear,
        Integer quarter,
        String currency,
        List<KpiCard> kpis,
        List<DepartmentSpend> spendByDepartment,
        List<ChartDataPoint> spendByCategory,
        List<MonthlyTrend> monthlyTrend,
        ApprovalSla approvalSla,
        List<TopVendor> topVendors,
        Instant cachedAt) {

    public ExecutiveDashboard {
        id = Objects.requireNonNull(id, "id must not be null");
        if (fiscalYear < 2000) {
            throw new IllegalArgumentException("fiscalYear is invalid");
        }
        if (quarter != null && (quarter < 1 || quarter > 4)) {
            throw new IllegalArgumentException("quarter must be 1..4");
        }
        currency = requireText(currency, "currency");
        kpis = List.copyOf(kpis == null ? List.of() : kpis);
        spendByDepartment = List.copyOf(spendByDepartment == null ? List.of() : spendByDepartment);
        spendByCategory = List.copyOf(spendByCategory == null ? List.of() : spendByCategory);
        monthlyTrend = List.copyOf(monthlyTrend == null ? List.of() : monthlyTrend);
        approvalSla = approvalSla == null ? new ApprovalSla(BigDecimal.ZERO, BigDecimal.ZERO, 0) : approvalSla;
        topVendors = List.copyOf(topVendors == null ? List.of() : topVendors);
        cachedAt = Objects.requireNonNull(cachedAt, "cachedAt must not be null");
    }

    public static ExecutiveDashboard empty(int fiscalYear, Integer quarter, Instant cachedAt) {
        String currency = "VND";
        return new ExecutiveDashboard(
                UUID.randomUUID(),
                fiscalYear,
                quarter,
                currency,
                List.of(
                        new KpiCard("analytics.kpi.totalSpendYtd", "0.0000", currency, null, KpiStatus.GOOD),
                        new KpiCard("analytics.kpi.approvedPrCount", "0", null, null, KpiStatus.GOOD),
                        new KpiCard("analytics.kpi.rfqSavings", "0.0000", currency, null, KpiStatus.GOOD)),
                List.of(),
                List.of(),
                List.of(),
                new ApprovalSla(BigDecimal.ZERO, BigDecimal.ZERO, 0),
                List.of(),
                cachedAt);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
