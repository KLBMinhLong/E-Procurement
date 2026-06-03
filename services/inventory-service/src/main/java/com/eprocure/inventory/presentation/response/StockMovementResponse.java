package com.eprocure.inventory.presentation.response;

import com.eprocure.inventory.domain.model.StockMovementType;
import java.time.Instant;
import java.util.UUID;

public record StockMovementResponse(
        UUID id,
        String itemCode,
        String itemName,
        UUID warehouseId,
        StockMovementType movementType,
        String quantity,
        String unit,
        String balanceAfter,
        String sourceRefType,
        UUID sourceRefId,
        PerformedByResponse performedBy,
        Instant performedAt,
        String notes) {

    public record PerformedByResponse(UUID id, String fullName) {
    }
}
