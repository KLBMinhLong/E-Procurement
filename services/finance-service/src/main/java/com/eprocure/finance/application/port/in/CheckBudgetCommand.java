package com.eprocure.finance.application.port.in;

import com.eprocure.finance.domain.model.vo.Money;
import java.util.Objects;
import java.util.UUID;

public record CheckBudgetCommand(
        UUID departmentId,
        int fiscalYear,
        String glAccountCode,
        Money requestAmount) {

    public CheckBudgetCommand {
        departmentId = Objects.requireNonNull(departmentId, "departmentId must not be null");
        requestAmount = Objects.requireNonNull(requestAmount, "requestAmount must not be null");
        glAccountCode = glAccountCode == null || glAccountCode.isBlank() ? null : glAccountCode.trim().toUpperCase();
    }
}
