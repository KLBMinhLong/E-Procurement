package com.eprocure.iam.presentation.delegation;

import com.eprocure.iam.domain.model.DelegationScope;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DelegationRequest(
        @NotNull UUID delegateId,
        @NotNull Instant startAt,
        @NotNull Instant endAt,
        @DecimalMin("0.0000") BigDecimal maxValue,
        @Size(min = 3, max = 3) String currency,
        List<@Size(max = 80) String> allowedCategories,
        DelegationScope scope) {
}
