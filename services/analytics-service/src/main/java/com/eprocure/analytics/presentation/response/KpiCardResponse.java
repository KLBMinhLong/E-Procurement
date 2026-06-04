package com.eprocure.analytics.presentation.response;

import com.eprocure.analytics.domain.model.KpiStatus;

public record KpiCardResponse(
        String label,
        String value,
        String unit,
        TrendResponse trend,
        KpiStatus status) {
}
