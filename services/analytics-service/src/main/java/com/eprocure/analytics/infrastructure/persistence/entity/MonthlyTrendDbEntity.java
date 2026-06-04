package com.eprocure.analytics.infrastructure.persistence.entity;

import com.eprocure.analytics.domain.model.MonthlyTrend;
import java.math.BigDecimal;

public class MonthlyTrendDbEntity {
    private String month;
    private BigDecimal spent;
    private BigDecimal budget;
    private Integer prCount;

    public MonthlyTrend toDomain() {
        return new MonthlyTrend(month, spent, budget, prCount == null ? 0 : prCount);
    }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }
    public BigDecimal getSpent() { return spent; }
    public void setSpent(BigDecimal spent) { this.spent = spent; }
    public BigDecimal getBudget() { return budget; }
    public void setBudget(BigDecimal budget) { this.budget = budget; }
    public Integer getPrCount() { return prCount; }
    public void setPrCount(Integer prCount) { this.prCount = prCount; }
}
