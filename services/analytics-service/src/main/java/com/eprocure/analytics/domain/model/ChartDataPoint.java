package com.eprocure.analytics.domain.model;

import java.math.BigDecimal;

public record ChartDataPoint(
        String label,
        BigDecimal value,
        BigDecimal value2) {

    public ChartDataPoint {
        label = requireText(label, "label");
        value = value == null ? BigDecimal.ZERO : value;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
