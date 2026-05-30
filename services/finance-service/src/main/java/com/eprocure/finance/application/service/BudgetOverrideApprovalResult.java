package com.eprocure.finance.application.service;

public record BudgetOverrideApprovalResult(BudgetOverrideApprovalView view, boolean replayed) {

    public static BudgetOverrideApprovalResult fresh(BudgetOverrideApprovalView view) {
        return new BudgetOverrideApprovalResult(view, false);
    }

    public static BudgetOverrideApprovalResult replayed(BudgetOverrideApprovalView view) {
        return new BudgetOverrideApprovalResult(view, true);
    }
}
