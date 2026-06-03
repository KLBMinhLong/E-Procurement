package com.eprocure.finance.presentation.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateInvoiceRequest(
        @NotBlank @Size(max = 100) String invoiceNumber,
        @NotNull UUID vendorId,
        @NotNull UUID poId,
        @NotNull LocalDate invoiceDate,
        @NotNull LocalDate dueDate,
        @NotEmpty @Valid List<LineItem> lineItems,
        List<UUID> attachmentIds) {

    public record LineItem(
            @NotBlank @Size(max = 500) String description,
            @NotNull @Positive BigDecimal quantity,
            @NotNull @DecimalMin("0.0000") BigDecimal unitPrice,
            @DecimalMin("0.0000") @DecimalMax("1.0000") BigDecimal taxRate) {
    }
}
