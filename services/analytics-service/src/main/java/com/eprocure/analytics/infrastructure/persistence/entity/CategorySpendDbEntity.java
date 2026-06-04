package com.eprocure.analytics.infrastructure.persistence.entity;

import com.eprocure.analytics.domain.model.ChartDataPoint;
import java.math.BigDecimal;

public class CategorySpendDbEntity {
    private String categoryCode;
    private BigDecimal spent;
    private BigDecimal budget;

    public ChartDataPoint toDomain() {
        return new ChartDataPoint(categoryCode, spent, budget);
    }

    public String getCategoryCode() { return categoryCode; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }
    public BigDecimal getSpent() { return spent; }
    public void setSpent(BigDecimal spent) { this.spent = spent; }
    public BigDecimal getBudget() { return budget; }
    public void setBudget(BigDecimal budget) { this.budget = budget; }
}
