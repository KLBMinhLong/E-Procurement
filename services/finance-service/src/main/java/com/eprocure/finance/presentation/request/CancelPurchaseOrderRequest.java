package com.eprocure.finance.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelPurchaseOrderRequest(
        @NotBlank @Size(min = 10, max = 1000) String reason) {
}
