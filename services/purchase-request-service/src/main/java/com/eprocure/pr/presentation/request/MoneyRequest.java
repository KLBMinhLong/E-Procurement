package com.eprocure.pr.presentation.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record MoneyRequest(
        @NotNull
        @DecimalMin(value = "0.0000")
        BigDecimal amount,

        @Pattern(regexp = "^[A-Z]{3}$")
        String currency) {
}
