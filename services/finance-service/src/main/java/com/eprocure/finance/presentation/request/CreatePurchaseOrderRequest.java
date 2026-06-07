package com.eprocure.finance.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record CreatePurchaseOrderRequest(
        @NotNull UUID prId,
        @NotNull UUID vendorId,
        @NotBlank String deliveryAddress,
        LocalDate deliveryDeadline,
        String paymentTerms,
        String notes) {
}
