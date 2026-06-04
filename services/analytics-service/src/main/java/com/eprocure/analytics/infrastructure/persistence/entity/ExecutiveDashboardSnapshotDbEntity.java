package com.eprocure.analytics.infrastructure.persistence.entity;

import com.eprocure.analytics.domain.model.ApprovalSla;
import com.eprocure.analytics.domain.model.ExecutiveDashboard;
import com.eprocure.analytics.domain.model.KpiCard;
import com.eprocure.analytics.domain.model.KpiStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ExecutiveDashboardSnapshotDbEntity {
    private UUID id;
    private Integer fiscalYear;
    private Integer quarter;
    private String currency;
    private BigDecimal totalSpent;
    private Integer approvedPrCount;
    private BigDecimal rfqSavings;
    private BigDecimal approvalOnTimePercent;
    private BigDecimal approvalAvgCycleHours;
    private Integer approvalOverdueCount;
    private Instant cachedAt;

    public ExecutiveDashboard toDomain(
            List<DepartmentSpendDbEntity> departmentRows,
            List<CategorySpendDbEntity> categoryRows,
            List<MonthlyTrendDbEntity> monthlyRows,
            List<TopVendorDbEntity> vendorRows) {
        return new ExecutiveDashboard(
                id,
                fiscalYear,
                quarter,
                currency,
                List.of(
                        new KpiCard("analytics.kpi.totalSpendYtd", moneyValue(totalSpent), currency, null, KpiStatus.GOOD),
                        new KpiCard("analytics.kpi.approvedPrCount", String.valueOf(approvedPrCount == null ? 0 : approvedPrCount), null, null, KpiStatus.GOOD),
                        new KpiCard("analytics.kpi.rfqSavings", moneyValue(rfqSavings), currency, null, KpiStatus.GOOD)),
                departmentRows.stream().map(DepartmentSpendDbEntity::toDomain).toList(),
                categoryRows.stream().map(CategorySpendDbEntity::toDomain).toList(),
                monthlyRows.stream().map(MonthlyTrendDbEntity::toDomain).toList(),
                new ApprovalSla(approvalOnTimePercent, approvalAvgCycleHours, approvalOverdueCount == null ? 0 : approvalOverdueCount),
                vendorRows.stream().map(TopVendorDbEntity::toDomain).toList(),
                cachedAt);
    }

    private String moneyValue(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).toPlainString();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Integer getFiscalYear() { return fiscalYear; }
    public void setFiscalYear(Integer fiscalYear) { this.fiscalYear = fiscalYear; }
    public Integer getQuarter() { return quarter; }
    public void setQuarter(Integer quarter) { this.quarter = quarter; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getTotalSpent() { return totalSpent; }
    public void setTotalSpent(BigDecimal totalSpent) { this.totalSpent = totalSpent; }
    public Integer getApprovedPrCount() { return approvedPrCount; }
    public void setApprovedPrCount(Integer approvedPrCount) { this.approvedPrCount = approvedPrCount; }
    public BigDecimal getRfqSavings() { return rfqSavings; }
    public void setRfqSavings(BigDecimal rfqSavings) { this.rfqSavings = rfqSavings; }
    public BigDecimal getApprovalOnTimePercent() { return approvalOnTimePercent; }
    public void setApprovalOnTimePercent(BigDecimal approvalOnTimePercent) { this.approvalOnTimePercent = approvalOnTimePercent; }
    public BigDecimal getApprovalAvgCycleHours() { return approvalAvgCycleHours; }
    public void setApprovalAvgCycleHours(BigDecimal approvalAvgCycleHours) { this.approvalAvgCycleHours = approvalAvgCycleHours; }
    public Integer getApprovalOverdueCount() { return approvalOverdueCount; }
    public void setApprovalOverdueCount(Integer approvalOverdueCount) { this.approvalOverdueCount = approvalOverdueCount; }
    public Instant getCachedAt() { return cachedAt; }
    public void setCachedAt(Instant cachedAt) { this.cachedAt = cachedAt; }
}
