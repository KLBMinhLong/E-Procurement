package com.eprocure.inventory.application.service;

import java.util.Objects;

public record StockAdjustmentResult(
        boolean replayed,
        StockMovementView movement) {

    public StockAdjustmentResult {
        movement = Objects.requireNonNull(movement, "movement must not be null");
    }

    public static StockAdjustmentResult fresh(StockMovementView movement) {
        return new StockAdjustmentResult(false, movement);
    }

    public static StockAdjustmentResult replayed(StockMovementView movement) {
        return new StockAdjustmentResult(true, movement);
    }
}
