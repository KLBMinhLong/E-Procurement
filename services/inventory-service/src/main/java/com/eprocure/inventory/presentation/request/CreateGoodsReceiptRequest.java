package com.eprocure.inventory.presentation.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateGoodsReceiptRequest(
        @NotNull UUID poId,
        @NotNull UUID warehouseId,
        Instant receivedAt,
        @NotEmpty List<@Valid LineItem> lineItems,
        @Size(max = 2000) String notes) {

    public record LineItem(
            @NotNull UUID poLineItemId,
            @NotNull @PositiveOrZero BigDecimal receivedQuantity,
            @PositiveOrZero BigDecimal rejectedQuantity,
            @Size(max = 1000) String rejectionReason,
            @Size(max = 100) String lotNumber) {
    }
}
