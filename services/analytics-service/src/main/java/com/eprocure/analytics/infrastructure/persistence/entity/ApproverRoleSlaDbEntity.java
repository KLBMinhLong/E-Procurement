package com.eprocure.analytics.infrastructure.persistence.entity;

import com.eprocure.analytics.domain.model.kpi.ApproverRoleSla;
import java.math.BigDecimal;

public class ApproverRoleSlaDbEntity {
    private String role;
    private BigDecimal compliancePct;
    private BigDecimal avgActionHours;
    private Integer overdueCount;

    public ApproverRoleSla toDomain() {
        return new ApproverRoleSla(role, compliancePct, avgActionHours, overdueCount == null ? 0 : overdueCount);
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public BigDecimal getCompliancePct() {
        return compliancePct;
    }

    public void setCompliancePct(BigDecimal compliancePct) {
        this.compliancePct = compliancePct;
    }

    public BigDecimal getAvgActionHours() {
        return avgActionHours;
    }

    public void setAvgActionHours(BigDecimal avgActionHours) {
        this.avgActionHours = avgActionHours;
    }

    public Integer getOverdueCount() {
        return overdueCount;
    }

    public void setOverdueCount(Integer overdueCount) {
        this.overdueCount = overdueCount;
    }
}
