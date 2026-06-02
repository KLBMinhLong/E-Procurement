package com.eprocure.vendor.domain.repository;

import com.eprocure.vendor.domain.model.Rfq;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

public interface RfqRepository {
    String nextRfqNumber();

    Optional<Rfq> findById(UUID id);

    Optional<Rfq> findByIdempotencyKey(UUID idempotencyKey);

    List<Rfq> findByFilter(RfqFilter filter);

    long countByFilter(RfqFilter filter);

    void save(Rfq rfq);

    void updateStatus(Rfq rfq);

    void markInvitationSubmitted(UUID rfqId, UUID vendorId, Instant submittedAt, UUID actorId);
}
