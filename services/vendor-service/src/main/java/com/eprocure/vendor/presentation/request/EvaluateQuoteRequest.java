package com.eprocure.vendor.presentation.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record EvaluateQuoteRequest(
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal evaluationScore,
        String evaluationNote) {
}
