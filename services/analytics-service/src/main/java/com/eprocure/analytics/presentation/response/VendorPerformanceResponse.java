package com.eprocure.analytics.presentation.response;

import java.math.BigDecimal;

public record VendorPerformanceResponse(
        String vendorName,
        BigDecimal onTimeDelivery,
        int qualityScore,
        int pendingOrders) {
}
