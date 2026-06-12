package com.eprocure.inventory.presentation.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateItemRequest(
        @NotBlank @Size(max = 20) String itemCode,
        @NotBlank @Size(max = 300) String name,
        @Size(max = 2000) String description,
        @NotBlank @Size(max = 50) String categoryCode,
        @NotBlank @Size(max = 20) String unit,
        @NotNull @Valid MoneyRequest unitPrice,
        UUID preferredVendorId,
        @PositiveOrZero BigDecimal reorderPoint) {

    public record MoneyRequest(
            @NotNull @PositiveOrZero BigDecimal amount,
            @Size(min = 3, max = 3) String currency) {
    }
}
