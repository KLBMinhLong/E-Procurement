package com.eprocure.inventory.presentation.response;

import java.util.List;

public record IssueOutStockResponse(List<StockMovementResponse> movements) {
    public IssueOutStockResponse {
        movements = List.copyOf(movements);
    }
}
