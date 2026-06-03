package com.eprocure.finance.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ConfirmPaymentRequest(
        @NotNull LocalDate paymentDate,
        @NotBlank @Size(max = 120) String paymentReference,
        @Positive BigDecimal paidAmount,
        @Size(max = 1000) String notes) {
}
