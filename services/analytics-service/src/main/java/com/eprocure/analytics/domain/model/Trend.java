package com.eprocure.analytics.domain.model;

import java.math.BigDecimal;

public record Trend(
        TrendDirection direction,
        BigDecimal percent,
        String vsLabel) {

    public Trend {
        direction = direction == null ? TrendDirection.FLAT : direction;
        percent = percent == null ? BigDecimal.ZERO : percent;
        vsLabel = normalize(vsLabel);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
