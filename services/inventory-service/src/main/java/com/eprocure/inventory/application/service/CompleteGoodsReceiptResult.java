package com.eprocure.inventory.application.service;

import com.eprocure.inventory.domain.model.GoodsReceiptStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record CompleteGoodsReceiptResult(
        GoodsReceiptStatus grStatus,
        int movementsCreated,
        List<StockUpdate> updatedStocks,
        boolean replayed) {

    public CompleteGoodsReceiptResult {
        grStatus = Objects.requireNonNull(grStatus, "grStatus must not be null");
        if (movementsCreated < 0) {
            throw new IllegalArgumentException("movementsCreated must not be negative");
        }
        updatedStocks = List.copyOf(Objects.requireNonNull(updatedStocks, "updatedStocks must not be null"));
    }

    public static CompleteGoodsReceiptResult fresh(
            GoodsReceiptStatus grStatus,
            int movementsCreated,
            List<StockUpdate> updatedStocks) {
        return new CompleteGoodsReceiptResult(grStatus, movementsCreated, updatedStocks, false);
    }

    public static CompleteGoodsReceiptResult replayed(
            GoodsReceiptStatus grStatus,
            int movementsCreated,
            List<StockUpdate> updatedStocks) {
        return new CompleteGoodsReceiptResult(grStatus, movementsCreated, updatedStocks, true);
    }

    public record StockUpdate(String itemCode, BigDecimal newQuantityOnHand) {
        public StockUpdate {
            if (itemCode == null || itemCode.isBlank()) {
                throw new IllegalArgumentException("itemCode must not be blank");
            }
            itemCode = itemCode.trim();
            newQuantityOnHand = Objects.requireNonNull(newQuantityOnHand, "newQuantityOnHand must not be null");
        }
    }
}
