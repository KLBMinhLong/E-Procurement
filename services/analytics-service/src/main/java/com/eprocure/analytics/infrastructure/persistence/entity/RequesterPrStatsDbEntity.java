package com.eprocure.analytics.infrastructure.persistence.entity;

import com.eprocure.analytics.domain.model.dashboard.MyPurchaseRequestStats;

public class RequesterPrStatsDbEntity {
    private int submitted;

    public MyPurchaseRequestStats toDomain() {
        return new MyPurchaseRequestStats(0, submitted, 0, 0, 0);
    }

    public int getSubmitted() {
        return submitted;
    }

    public void setSubmitted(int submitted) {
        this.submitted = submitted;
    }
}
