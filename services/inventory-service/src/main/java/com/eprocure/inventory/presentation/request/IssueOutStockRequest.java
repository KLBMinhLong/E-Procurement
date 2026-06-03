package com.eprocure.inventory.presentation.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record IssueOutStockRequest(
        @NotNull UUID warehouseId,
        UUID prId,
        @NotNull UUID recipientId,
        @NotEmpty @Valid List<LineItem> items,
        @Size(max = 2000) String notes) {

    public record LineItem(
            @NotBlank @Size(max = 20) String itemCode,
            @NotNull @Positive BigDecimal quantity,
            @NotBlank @Size(max = 20) String unit) {
    }
}
