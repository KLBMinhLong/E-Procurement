package com.eprocure.iam.infrastructure.persistence.entity;

import com.eprocure.iam.domain.model.DelegationScope;
import com.eprocure.iam.domain.model.DelegationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class DelegationDbEntity {
    public UUID id;
    public UUID delegatorId;
    public UUID delegateId;
    public Instant startAt;
    public Instant endAt;
    public BigDecimal maxValue;
    public String currency;
    public String allowedCategoriesJson;
    public DelegationScope scope;
    public DelegationStatus status;
    public Instant createdAt;
}
