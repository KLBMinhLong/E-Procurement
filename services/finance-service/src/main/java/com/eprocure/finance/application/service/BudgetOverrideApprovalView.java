package com.eprocure.finance.application.service;

import com.eprocure.finance.domain.model.BudgetOverrideApproval;
import com.eprocure.finance.domain.model.BudgetOverrideStatus;
import com.eprocure.finance.domain.model.vo.Money;
import java.time.Instant;
import java.util.UUID;

public record BudgetOverrideApprovalView(
        UUID id,
        UUID budgetId,
        UUID purchaseRequestId,
        Money overrideAmount,
        String overrideReason,
        UUID approvedBy,
        Instant approvedAt,
        BudgetOverrideStatus status) {

    public static BudgetOverrideApprovalView from(BudgetOverrideApproval approval) {
        return new BudgetOverrideApprovalView(
                approval.id(),
                approval.budgetId(),
                approval.purchaseRequestId(),
                approval.money(),
                approval.reason(),
                approval.approvedBy(),
                approval.approvedAt(),
                approval.status());
    }
}
