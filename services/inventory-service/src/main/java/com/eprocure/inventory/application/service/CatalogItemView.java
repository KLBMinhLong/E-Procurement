package com.eprocure.inventory.application.service;

import com.eprocure.inventory.domain.model.Item;
import com.eprocure.inventory.domain.repository.ItemRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CatalogItemView(
        UUID id,
        String itemCode,
        String name,
        String description,
        String categoryCode,
        String unit,
        BigDecimal unitPrice,
        String currency,
        UUID preferredVendorId,
        BigDecimal reorderPoint,
        boolean active,
        List<StockSummaryView> stockSummary) {

    public CatalogItemView {
        stockSummary = List.copyOf(stockSummary);
    }

    public static CatalogItemView from(Item item, List<ItemRepository.StockSummary> stockSummary) {
        return new CatalogItemView(
                item.id(),
                item.itemCode(),
                item.name(),
                item.description(),
                item.categoryCode(),
                item.unit(),
                item.unitPrice(),
                item.currency(),
                item.preferredVendorId(),
                item.reorderPoint(),
                item.active(),
                stockSummary.stream().map(StockSummaryView::from).toList());
    }

    public record StockSummaryView(
            UUID warehouseId,
            String warehouseName,
            BigDecimal quantityOnHand,
            String unit) {

        public static StockSummaryView from(ItemRepository.StockSummary stockSummary) {
            return new StockSummaryView(
                    stockSummary.warehouseId(),
                    stockSummary.warehouseName(),
                    stockSummary.quantityOnHand(),
                    stockSummary.unit());
        }
    }
}
