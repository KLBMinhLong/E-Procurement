package com.eprocure.finance.domain.model;

import com.eprocure.finance.domain.model.vo.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

public record BudgetLedgerSummary(
        UUID id,
        UUID departmentId,
        int fiscalYear,
        Integer quarter,
        String glAccountCode,
        Money allocated,
        Money committed,
        Money spent,
        BudgetStatus status) {

    private static final BigDecimal WARNING_AVAILABLE_RATIO = new BigDecimal("0.20");

    public BudgetLedgerSummary {
        id = Objects.requireNonNull(id, "id must not be null");
        departmentId = Objects.requireNonNull(departmentId, "departmentId must not be null");
        glAccountCode = Objects.requireNonNull(glAccountCode, "glAccountCode must not be null");
        allocated = Objects.requireNonNull(allocated, "allocated must not be null");
        committed = Objects.requireNonNull(committed, "committed must not be null");
        spent = Objects.requireNonNull(spent, "spent must not be null");
        status = Objects.requireNonNull(status, "status must not be null");
    }

    public Money available() {
        return allocated.subtract(committed).subtract(spent);
    }

    public BudgetCheckStatus check(Money requestAmount) {
        Objects.requireNonNull(requestAmount, "requestAmount must not be null");
        Money availableAfter = available().subtract(requestAmount);
        if (availableAfter.isNegative()) {
            return BudgetCheckStatus.FAIL;
        }
        if (allocated.amount().signum() == 0) {
            return BudgetCheckStatus.FAIL;
        }
        BigDecimal ratio = availableAfter.amount().divide(allocated.amount(), 6, RoundingMode.HALF_UP);
        return ratio.compareTo(WARNING_AVAILABLE_RATIO) < 0
                ? BudgetCheckStatus.WARNING
                : BudgetCheckStatus.PASS;
    }
}
