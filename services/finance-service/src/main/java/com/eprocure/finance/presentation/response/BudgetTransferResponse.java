package com.eprocure.finance.presentation.response;

import java.time.Instant;
import java.util.UUID;

public record BudgetTransferResponse(
        UUID id,
        UUID sourceBudgetId,
        UUID targetBudgetId,
        String amount,
        String currency,
        String reason,
        UUID approvedBy,
        Instant approvedAt,
        BudgetDashboardResponse sourceDashboard,
        BudgetDashboardResponse targetDashboard) {
}
