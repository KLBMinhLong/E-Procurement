package com.eprocure.inventory.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record AdjustStockRequest(
        @NotNull UUID warehouseId,
        @NotBlank @Size(max = 20) String itemCode,
        @NotNull @PositiveOrZero BigDecimal newQuantity,
        @NotBlank @Size(max = 20) String unit,
        @NotBlank @Size(min = 10, max = 2000) String reason) {
}
