package com.eprocure.vendor.application.service;

import com.eprocure.vendor.domain.model.VendorScorecard;
import java.math.BigDecimal;
import java.time.Instant;

public record VendorScorecardView(
        int qualityScore,
        int deliveryScore,
        int priceScore,
        int responsivenessScore,
        int overallScore,
        Instant lastEvaluatedAt,
        int totalOrders,
        BigDecimal onTimeDeliveryRate) {

    public static VendorScorecardView from(VendorScorecard scorecard) {
        return new VendorScorecardView(
                scorecard.qualityScore(),
                scorecard.deliveryScore(),
                scorecard.priceScore(),
                scorecard.responsivenessScore(),
                scorecard.overallScore(),
                scorecard.lastEvaluatedAt(),
                scorecard.totalOrders(),
                scorecard.onTimeDeliveryRate());
    }
}
