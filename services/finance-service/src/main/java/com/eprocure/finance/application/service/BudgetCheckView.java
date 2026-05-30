package com.eprocure.finance.application.service;

import com.eprocure.finance.domain.model.BudgetCheckStatus;
import com.eprocure.finance.domain.model.vo.Money;
import java.util.UUID;

public record BudgetCheckView(
        UUID budgetId,
        UUID departmentId,
        int fiscalYear,
        Integer quarter,
        String glAccountCode,
        Money allocated,
        Money committed,
        Money spent,
        Money available,
        BudgetCheckStatus status,
        String warningMessage) {
}
