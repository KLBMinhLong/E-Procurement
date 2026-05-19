package com.eprocure.pr.domain.model.vo;

import java.util.List;

public record InventoryCheckResult(
        List<InventoryStockItem> itemsWithStock,
        String suggestion) {

    public InventoryCheckResult {
        itemsWithStock = itemsWithStock == null ? List.of() : List.copyOf(itemsWithStock);
        suggestion = normalizeOptionalText(suggestion);
    }

    public static InventoryCheckResult empty() {
        return new InventoryCheckResult(List.of(), null);
    }

    private static String normalizeOptionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
