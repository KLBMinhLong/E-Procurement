package com.eprocure.analytics.infrastructure.persistence.entity;

import com.eprocure.analytics.domain.model.DepartmentSpend;
import com.eprocure.analytics.domain.model.KpiStatus;
import java.math.BigDecimal;

public class DepartmentSpendDbEntity {
    private String departmentCode;
    private String departmentName;
    private BigDecimal spent;
    private BigDecimal budget;
    private BigDecimal utilization;
    private KpiStatus status;

    public DepartmentSpend toDomain() {
        return new DepartmentSpend(departmentCode, departmentName, spent, budget, utilization, status);
    }

    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public BigDecimal getSpent() { return spent; }
    public void setSpent(BigDecimal spent) { this.spent = spent; }
    public BigDecimal getBudget() { return budget; }
    public void setBudget(BigDecimal budget) { this.budget = budget; }
    public BigDecimal getUtilization() { return utilization; }
    public void setUtilization(BigDecimal utilization) { this.utilization = utilization; }
    public KpiStatus getStatus() { return status; }
    public void setStatus(KpiStatus status) { this.status = status; }
}
