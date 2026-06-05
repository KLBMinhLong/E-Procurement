package com.eprocure.analytics.infrastructure.persistence.entity;

import java.math.BigDecimal;

public class CycleTimeSummaryDbEntity {
    private BigDecimal avgCycleHours;
    private BigDecimal medianCycleHours;
    private BigDecimal p95CycleHours;

    public BigDecimal getAvgCycleHours() {
        return avgCycleHours;
    }

    public void setAvgCycleHours(BigDecimal avgCycleHours) {
        this.avgCycleHours = avgCycleHours;
    }

    public BigDecimal getMedianCycleHours() {
        return medianCycleHours;
    }

    public void setMedianCycleHours(BigDecimal medianCycleHours) {
        this.medianCycleHours = medianCycleHours;
    }

    public BigDecimal getP95CycleHours() {
        return p95CycleHours;
    }

    public void setP95CycleHours(BigDecimal p95CycleHours) {
        this.p95CycleHours = p95CycleHours;
    }
}
