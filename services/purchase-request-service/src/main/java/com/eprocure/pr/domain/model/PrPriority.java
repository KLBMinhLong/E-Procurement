package com.eprocure.pr.domain.model;

public enum PrPriority {
    NORMAL,
    URGENT,
    EMERGENCY;

    public boolean requiresUrgencyReason() {
        return this == URGENT || this == EMERGENCY;
    }
}
