package com.eprocure.analytics.infrastructure.persistence.entity;

import com.eprocure.analytics.domain.model.KpiCard;
import com.eprocure.analytics.domain.model.KpiStatus;
import com.eprocure.analytics.domain.model.dashboard.BudgetStatus;
import com.eprocure.analytics.domain.model.dashboard.PendingApprovals;
import java.math.BigDecimal;

public class ManagerDashboardMetricsDbEntity {
    private Integer submittedPrCount;
    private BigDecimal submittedPrTotal;
    private Integer slaBreachCount;
    private String currency;

    public KpiCard submittedPrCountKpi() {
        int count = submittedPrCount == null ? 0 : submittedPrCount;
        return new KpiCard("analytics.kpi.submittedPrCount", String.valueOf(count), null, null, KpiStatus.GOOD);
    }

    public KpiCard submittedPrTotalKpi() {
        String amount = (submittedPrTotal == null ? BigDecimal.ZERO : submittedPrTotal).toPlainString();
        String resolvedCurrency = currency == null || currency.isBlank() ? "VND" : currency;
        return new KpiCard("analytics.kpi.submittedPrTotal", amount, resolvedCurrency, null, KpiStatus.GOOD);
    }

    public KpiCard slaBreachCountKpi() {
        int breaches = slaBreachCount == null ? 0 : slaBreachCount;
        KpiStatus status = breaches > 0 ? KpiStatus.WARNING : KpiStatus.GOOD;
        return new KpiCard("analytics.kpi.slaBreachCount", String.valueOf(breaches), null, null, status);
    }

    public PendingApprovals toPendingApprovals() {
        int breaches = slaBreachCount == null ? 0 : slaBreachCount;
        int count = submittedPrCount == null ? 0 : submittedPrCount;
        return new PendingApprovals(count, breaches, 0);
    }

    public BudgetStatus toBudgetStatus() {
        BigDecimal total = submittedPrTotal == null ? BigDecimal.ZERO : submittedPrTotal;
        return new BudgetStatus(BigDecimal.ZERO, total, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null);
    }

    public Integer getSubmittedPrCount() {
        return submittedPrCount;
    }

    public void setSubmittedPrCount(Integer submittedPrCount) {
        this.submittedPrCount = submittedPrCount;
    }

    public BigDecimal getSubmittedPrTotal() {
        return submittedPrTotal;
    }

    public void setSubmittedPrTotal(BigDecimal submittedPrTotal) {
        this.submittedPrTotal = submittedPrTotal;
    }

    public Integer getSlaBreachCount() {
        return slaBreachCount;
    }

    public void setSlaBreachCount(Integer slaBreachCount) {
        this.slaBreachCount = slaBreachCount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
