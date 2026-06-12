package com.eprocure.inventory.presentation.response;

import java.util.List;
import java.util.UUID;

public record CatalogItemResponse(
        UUID id,
        String itemCode,
        String name,
        String description,
        String categoryCode,
        String unit,
        MoneyResponse unitPrice,
        UUID preferredVendorId,
        String reorderPoint,
        boolean isActive,
        List<StockSummaryResponse> stockSummary) {

    public record MoneyResponse(String amount, String currency) {
    }

    public record StockSummaryResponse(
            UUID warehouseId,
            String warehouseName,
            String quantityOnHand,
            String unit) {
    }
}
