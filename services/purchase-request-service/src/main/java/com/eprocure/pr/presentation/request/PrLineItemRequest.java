package com.eprocure.pr.presentation.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record PrLineItemRequest(
        String itemCode,

        @NotBlank
        @Size(max = 300)
        String itemName,

        String description,

        @NotBlank
        String categoryCode,

        @Valid
        @NotNull
        QuantityRequest quantity,

        @Valid
        @NotNull
        MoneyRequest unitPrice,

        UUID preferredVendorId,
        String specifications,

        @NotBlank
        @Size(max = 10)
        String glAccountCode,

        boolean isFromCatalog) {
}
