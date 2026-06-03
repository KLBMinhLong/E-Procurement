package com.eprocure.finance.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdatePurchaseOrderDraftRequest(
        @NotBlank @Size(max = 1000) String deliveryAddress,
        LocalDate deliveryDeadline,
        @Size(max = 100) String paymentTerms) {
}
