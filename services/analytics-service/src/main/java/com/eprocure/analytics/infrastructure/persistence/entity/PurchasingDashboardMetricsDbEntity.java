package com.eprocure.analytics.infrastructure.persistence.entity;

import com.eprocure.analytics.domain.model.KpiCard;
import com.eprocure.analytics.domain.model.KpiStatus;
import com.eprocure.analytics.domain.model.dashboard.PoPipeline;
import com.eprocure.analytics.domain.model.dashboard.PurchasingDashboard;
import com.eprocure.analytics.domain.model.dashboard.VendorPerformance;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class PurchasingDashboardMetricsDbEntity {
    private Integer issuedPoCount;
    private BigDecimal issuedPoTotal;
    private Integer matchedInvoiceCount;
    private String currency;

    public PurchasingDashboard toDomain(Instant cachedAt, List<VendorPerformance> vendorPerformance) {
        int sentToVendor = issuedPoCount == null ? 0 : issuedPoCount;
        int matchedInvoices = matchedInvoiceCount == null ? 0 : matchedInvoiceCount;
        String resolvedCurrency = currency == null || currency.isBlank() ? "VND" : currency;
        return new PurchasingDashboard(
                List.of(
                        new KpiCard("analytics.kpi.issuedPoCount", String.valueOf(sentToVendor), null, null, KpiStatus.GOOD),
                        new KpiCard("analytics.kpi.issuedPoTotal", money(issuedPoTotal), resolvedCurrency, null, KpiStatus.GOOD),
                        new KpiCard("analytics.kpi.matchedInvoiceCount", String.valueOf(matchedInvoices), null, null, KpiStatus.GOOD)),
                new PoPipeline(0, 0, sentToVendor, 0),
                0,
                0,
                0,
                vendorPerformance,
                cachedAt);
    }

    private String money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).toPlainString();
    }

    public Integer getIssuedPoCount() {
        return issuedPoCount;
    }

    public void setIssuedPoCount(Integer issuedPoCount) {
        this.issuedPoCount = issuedPoCount;
    }

    public BigDecimal getIssuedPoTotal() {
        return issuedPoTotal;
    }

    public void setIssuedPoTotal(BigDecimal issuedPoTotal) {
        this.issuedPoTotal = issuedPoTotal;
    }

    public Integer getMatchedInvoiceCount() {
        return matchedInvoiceCount;
    }

    public void setMatchedInvoiceCount(Integer matchedInvoiceCount) {
        this.matchedInvoiceCount = matchedInvoiceCount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
