package com.eprocure.iam.application.service;

import com.eprocure.iam.domain.model.DelegationScope;
import com.eprocure.iam.domain.model.DelegationStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DelegationDetailView(
        UUID id,
        UserSummaryView delegator,
        UserSummaryView delegate,
        Instant startAt,
        Instant endAt,
        String maxValue,
        String currency,
        List<String> allowedCategories,
        DelegationScope scope,
        DelegationStatus status,
        Instant createdAt) {
    public DelegationDetailView {
        allowedCategories = allowedCategories == null ? null : List.copyOf(allowedCategories);
    }
}
