package com.eprocure.inventory.application.service;

import com.eprocure.inventory.domain.model.StockMovementHistory;
import com.eprocure.inventory.domain.model.StockMovementType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StockMovementView(
        UUID id,
        String itemCode,
        String itemName,
        UUID warehouseId,
        StockMovementType movementType,
        BigDecimal quantity,
        String unit,
        BigDecimal balanceAfter,
        String sourceRefType,
        UUID sourceRefId,
        ActorView performedBy,
        Instant performedAt,
        String notes) {

    public static StockMovementView from(StockMovementHistory movement) {
        return new StockMovementView(
                movement.id(),
                movement.itemCode(),
                movement.itemName(),
                movement.warehouseId(),
                movement.movementType(),
                movement.quantity(),
                movement.unit(),
                movement.balanceAfter(),
                movement.sourceRefType(),
                movement.sourceRefId(),
                new ActorView(movement.performedBy(), movement.performedByFullName()),
                movement.performedAt(),
                movement.notes());
    }

    public record ActorView(UUID id, String fullName) {
    }
}
