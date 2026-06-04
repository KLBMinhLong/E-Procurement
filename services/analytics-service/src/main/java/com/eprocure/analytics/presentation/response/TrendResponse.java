package com.eprocure.analytics.presentation.response;

import com.eprocure.analytics.domain.model.TrendDirection;
import java.math.BigDecimal;

public record TrendResponse(
        TrendDirection direction,
        BigDecimal percent,
        String vsLabel) {
}
