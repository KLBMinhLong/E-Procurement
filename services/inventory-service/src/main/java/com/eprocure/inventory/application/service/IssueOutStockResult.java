package com.eprocure.inventory.application.service;

import java.util.List;

public record IssueOutStockResult(
        boolean replayed,
        List<StockMovementView> movements) {

    public IssueOutStockResult {
        movements = List.copyOf(movements);
    }

    public static IssueOutStockResult fresh(List<StockMovementView> movements) {
        return new IssueOutStockResult(false, movements);
    }

    public static IssueOutStockResult replayed(List<StockMovementView> movements) {
        return new IssueOutStockResult(true, movements);
    }
}
