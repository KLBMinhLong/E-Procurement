package com.eprocure.analytics.domain.model;

import java.math.BigDecimal;

public record TopVendor(
        String vendorName,
        BigDecimal totalSpent,
        int orderCount,
        BigDecimal avgScore) {

    public TopVendor {
        vendorName = requireText(vendorName, "vendorName");
        totalSpent = totalSpent == null ? BigDecimal.ZERO : totalSpent;
        avgScore = avgScore == null ? BigDecimal.ZERO : avgScore;
        if (orderCount < 0) {
            throw new IllegalArgumentException("orderCount must not be negative");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
