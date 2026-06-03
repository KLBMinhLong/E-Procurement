package com.eprocure.finance.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DisputeInvoiceRequest(@NotBlank @Size(min = 20, max = 2000) String reason) {
}
