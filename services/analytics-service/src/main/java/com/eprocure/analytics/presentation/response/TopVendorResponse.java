package com.eprocure.analytics.presentation.response;

import java.math.BigDecimal;

public record TopVendorResponse(
        String vendorName,
        String totalSpent,
        int orderCount,
        BigDecimal avgScore) {
}
