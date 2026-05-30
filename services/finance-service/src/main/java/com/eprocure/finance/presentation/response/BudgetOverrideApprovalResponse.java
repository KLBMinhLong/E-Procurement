package com.eprocure.finance.presentation.response;

import com.eprocure.finance.domain.model.BudgetOverrideStatus;
import java.time.Instant;
import java.util.UUID;

public record BudgetOverrideApprovalResponse(
        UUID id,
        UUID budgetId,
        UUID prId,
        String overrideAmount,
        String currency,
        String overrideReason,
        UUID approvedBy,
        Instant approvedAt,
        BudgetOverrideStatus status) {
}
