package com.eprocure.analytics.infrastructure.persistence.entity;

import com.eprocure.analytics.domain.model.kpi.WorstApproverSla;
import java.math.BigDecimal;

public class WorstApproverSlaDbEntity {
    private String approverName;
    private Integer overdueCount;
    private BigDecimal compliancePct;

    public WorstApproverSla toDomain() {
        return new WorstApproverSla(approverName, overdueCount == null ? 0 : overdueCount, compliancePct);
    }

    public String getApproverName() {
        return approverName;
    }

    public void setApproverName(String approverName) {
        this.approverName = approverName;
    }

    public Integer getOverdueCount() {
        return overdueCount;
    }

    public void setOverdueCount(Integer overdueCount) {
        this.overdueCount = overdueCount;
    }

    public BigDecimal getCompliancePct() {
        return compliancePct;
    }

    public void setCompliancePct(BigDecimal compliancePct) {
        this.compliancePct = compliancePct;
    }
}
