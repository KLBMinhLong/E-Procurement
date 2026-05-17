package com.eprocure.iam.application.port.in;

import com.eprocure.iam.domain.model.DelegationScope;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateDelegationCommand(
        UUID delegatorId,
        UUID delegateId,
        Instant startAt,
        Instant endAt,
        BigDecimal maxValue,
        String currency,
        List<String> allowedCategories,
        DelegationScope scope) {
    public CreateDelegationCommand {
        allowedCategories = allowedCategories == null ? null : List.copyOf(allowedCategories);
    }
}
