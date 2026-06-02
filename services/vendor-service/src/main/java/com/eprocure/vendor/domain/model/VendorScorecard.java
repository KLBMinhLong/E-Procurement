package com.eprocure.vendor.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record VendorScorecard(
        int qualityScore,
        int deliveryScore,
        int priceScore,
        int responsivenessScore,
        int overallScore,
        Instant lastEvaluatedAt,
        int totalOrders,
        BigDecimal onTimeDeliveryRate) {

    public VendorScorecard {
        validateScore(qualityScore, "qualityScore");
        validateScore(deliveryScore, "deliveryScore");
        validateScore(priceScore, "priceScore");
        validateScore(responsivenessScore, "responsivenessScore");
        validateScore(overallScore, "overallScore");
        if (totalOrders < 0) {
            throw new IllegalArgumentException("totalOrders must not be negative");
        }
        if (onTimeDeliveryRate == null) {
            onTimeDeliveryRate = BigDecimal.ZERO;
        }
        if (onTimeDeliveryRate.signum() < 0 || onTimeDeliveryRate.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("onTimeDeliveryRate must be between 0 and 100");
        }
    }

    private static void validateScore(int score, String fieldName) {
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException(fieldName + " must be between 0 and 100");
        }
    }
}
