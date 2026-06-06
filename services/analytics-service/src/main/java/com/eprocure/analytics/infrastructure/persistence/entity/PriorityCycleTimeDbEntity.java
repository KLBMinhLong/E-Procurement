package com.eprocure.analytics.infrastructure.persistence.entity;

import com.eprocure.analytics.domain.model.kpi.PriorityCycleTime;
import java.math.BigDecimal;

public class PriorityCycleTimeDbEntity {
    private String priority;
    private BigDecimal avgHours;

    public PriorityCycleTime toDomain() {
        return new PriorityCycleTime(priority, avgHours);
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public BigDecimal getAvgHours() {
        return avgHours;
    }

    public void setAvgHours(BigDecimal avgHours) {
        this.avgHours = avgHours;
    }
}
