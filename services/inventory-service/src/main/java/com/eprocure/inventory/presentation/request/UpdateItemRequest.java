package com.eprocure.inventory.presentation.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record UpdateItemRequest(
        @Size(max = 300) String name,
        @Size(max = 2000) String description,
        @Valid MoneyRequest unitPrice,
        UUID preferredVendorId,
        @PositiveOrZero BigDecimal reorderPoint,
        Boolean isActive) {

    public record MoneyRequest(
            @PositiveOrZero BigDecimal amount,
            @Size(min = 3, max = 3) String currency) {
    }
}
