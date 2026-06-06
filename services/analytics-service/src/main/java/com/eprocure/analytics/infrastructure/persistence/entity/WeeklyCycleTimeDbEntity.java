package com.eprocure.analytics.infrastructure.persistence.entity;

import com.eprocure.analytics.domain.model.kpi.WeeklyCycleTime;
import java.math.BigDecimal;

public class WeeklyCycleTimeDbEntity {
    private String week;
    private BigDecimal avgHours;

    public WeeklyCycleTime toDomain() {
        return new WeeklyCycleTime(week, avgHours);
    }

    public String getWeek() {
        return week;
    }

    public void setWeek(String week) {
        this.week = week;
    }

    public BigDecimal getAvgHours() {
        return avgHours;
    }

    public void setAvgHours(BigDecimal avgHours) {
        this.avgHours = avgHours;
    }
}
