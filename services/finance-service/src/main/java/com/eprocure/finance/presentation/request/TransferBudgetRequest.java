package com.eprocure.finance.presentation.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record TransferBudgetRequest(
        @NotNull UUID targetBudgetId,
        @NotNull @DecimalMin(value = "0.0001") @Digits(integer = 15, fraction = 4) BigDecimal amount,
        @Size(min = 3, max = 3) String currency,
        @NotNull @Size(min = 20, max = 1000) String reason) {
}
