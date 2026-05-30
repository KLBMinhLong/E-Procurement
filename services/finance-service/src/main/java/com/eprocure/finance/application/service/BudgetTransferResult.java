package com.eprocure.finance.application.service;

public record BudgetTransferResult(BudgetTransferView view, boolean replayed) {

    public static BudgetTransferResult fresh(BudgetTransferView view) {
        return new BudgetTransferResult(view, false);
    }

    public static BudgetTransferResult replayed(BudgetTransferView view) {
        return new BudgetTransferResult(view, true);
    }
}
