package com.eprocure.finance.presentation.request;

import jakarta.validation.constraints.Size;

public record ApproveInvoiceRequest(@Size(max = 1000) String comment) {
}
