package com.eprocure.vendor.presentation.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SubmitVendorQuoteRequest(
        @NotNull UUID vendorId,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotNull @FutureOrPresent LocalDate validUntil,
        @NotEmpty List<@Valid QuoteLineItemRequest> lineItems,
        String paymentTerms,
        String notes) {

    public record QuoteLineItemRequest(
            @NotNull UUID rfqLineItemId,
            @NotNull @DecimalMin(value = "0.0001") BigDecimal unitPrice,
            @Min(0) Integer deliveryDays,
            String warranty) {
    }
}
