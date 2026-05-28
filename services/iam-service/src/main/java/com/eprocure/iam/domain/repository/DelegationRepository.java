package com.eprocure.iam.domain.repository;

import com.eprocure.iam.domain.model.Delegation;
import com.eprocure.iam.domain.model.DelegationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DelegationRepository {
    Optional<Delegation> findById(UUID id);

    List<Delegation> findByDelegatorId(UUID delegatorId);

    Optional<Delegation> findActiveForApproval(
            UUID delegatorId,
            UUID requesterDepartmentId,
            BigDecimal totalAmount,
            String currency,
            List<String> categories,
            Instant effectiveAt);

    boolean hasActiveOverlap(UUID delegatorId, Instant startAt, Instant endAt);

    Optional<String> findOrgPathByUserId(UUID userId);

    void save(Delegation delegation, UUID actorId);

    void updateStatus(UUID delegationId, DelegationStatus status, UUID actorId);
}
