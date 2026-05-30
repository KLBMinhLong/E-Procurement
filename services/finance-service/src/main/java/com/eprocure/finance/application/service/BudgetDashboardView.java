package com.eprocure.finance.application.service;

import com.eprocure.finance.domain.model.BudgetLedgerSummary;
import com.eprocure.finance.domain.model.BudgetStatus;
import com.eprocure.finance.domain.model.vo.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

public record BudgetDashboardView(
        UUID id,
        UUID departmentId,
        int fiscalYear,
        Integer quarter,
        String glAccountCode,
        Money allocated,
        Money committed,
        Money spent,
        Money available,
        BigDecimal availablePercent,
        Money burnRatePerMonth,
        LocalDate forecastExhaustedAt,
        BudgetStatus status) {

    public static BudgetDashboardView from(BudgetLedgerSummary summary) {
        Money available = summary.available();
        return new BudgetDashboardView(
                summary.id(),
                summary.departmentId(),
                summary.fiscalYear(),
                summary.quarter(),
                summary.glAccountCode(),
                summary.allocated(),
                summary.committed(),
                summary.spent(),
                available,
                availablePercent(available, summary.allocated()),
                null,
                null,
                summary.status());
    }

    private static BigDecimal availablePercent(Money available, Money allocated) {
        if (allocated.amount().signum() == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return available.amount()
                .multiply(new BigDecimal("100"))
                .divide(allocated.amount(), 2, RoundingMode.HALF_UP);
    }
}
