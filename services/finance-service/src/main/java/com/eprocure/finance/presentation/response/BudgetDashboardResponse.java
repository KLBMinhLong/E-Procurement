package com.eprocure.finance.presentation.response;

import com.eprocure.finance.domain.model.BudgetStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BudgetDashboardResponse(
        UUID id,
        UUID departmentId,
        int fiscalYear,
        Integer quarter,
        String glAccountCode,
        String allocated,
        String committed,
        String spent,
        String available,
        BigDecimal availablePercent,
        String burnRatePerMonth,
        LocalDate forecastExhaustedAt,
        BudgetStatus status) {
}
