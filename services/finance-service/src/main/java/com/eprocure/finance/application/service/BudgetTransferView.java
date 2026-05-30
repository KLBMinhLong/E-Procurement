package com.eprocure.finance.application.service;

import com.eprocure.finance.domain.model.BudgetTransfer;
import com.eprocure.finance.domain.model.vo.Money;
import java.time.Instant;
import java.util.UUID;

public record BudgetTransferView(
        UUID id,
        UUID sourceBudgetId,
        UUID targetBudgetId,
        Money amount,
        String reason,
        UUID approvedBy,
        Instant approvedAt,
        BudgetDashboardView sourceDashboard,
        BudgetDashboardView targetDashboard) {

    public static BudgetTransferView from(
            BudgetTransfer transfer,
            BudgetDashboardView sourceDashboard,
            BudgetDashboardView targetDashboard) {
        return new BudgetTransferView(
                transfer.id(),
                transfer.sourceBudgetId(),
                transfer.targetBudgetId(),
                transfer.money(),
                transfer.reason(),
                transfer.approvedBy(),
                transfer.approvedAt(),
                sourceDashboard,
                targetDashboard);
    }
}
