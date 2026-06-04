package com.eprocure.analytics.domain.model;

import java.math.BigDecimal;

public record MonthlyTrend(
        String month,
        BigDecimal spent,
        BigDecimal budget,
        int prCount) {

    public MonthlyTrend {
        month = requireText(month, "month");
        spent = spent == null ? BigDecimal.ZERO : spent;
        budget = budget == null ? BigDecimal.ZERO : budget;
        if (prCount < 0) {
            throw new IllegalArgumentException("prCount must not be negative");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
