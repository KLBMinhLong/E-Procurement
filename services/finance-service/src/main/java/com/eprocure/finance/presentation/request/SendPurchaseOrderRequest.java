package com.eprocure.finance.presentation.request;

import jakarta.validation.constraints.Size;

public record SendPurchaseOrderRequest(
        @Size(max = 1000) String additionalNote) {
}
