package com.eprocure.vendor.presentation.response;

import java.math.BigDecimal;
import java.time.Instant;

public record VendorScorecardResponse(
        int qualityScore,
        int deliveryScore,
        int priceScore,
        int responsivenessScore,
        int overallScore,
        Instant lastEvaluatedAt,
        int totalOrders,
        BigDecimal onTimeDeliveryRate) {
}
