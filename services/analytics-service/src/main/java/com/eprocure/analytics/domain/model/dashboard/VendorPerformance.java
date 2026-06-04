package com.eprocure.analytics.domain.model.dashboard;

import java.math.BigDecimal;

public record VendorPerformance(
        String vendorName,
        BigDecimal onTimeDelivery,
        int qualityScore,
        int pendingOrders) {

    public VendorPerformance {
        vendorName = vendorName == null ? "" : vendorName.trim();
        onTimeDelivery = onTimeDelivery == null ? BigDecimal.ZERO : onTimeDelivery;
        qualityScore = Math.max(qualityScore, 0);
        pendingOrders = Math.max(pendingOrders, 0);
    }
}
