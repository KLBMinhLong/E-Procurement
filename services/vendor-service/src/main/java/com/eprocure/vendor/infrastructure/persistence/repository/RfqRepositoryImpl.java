package com.eprocure.vendor.infrastructure.persistence.repository;

import com.eprocure.vendor.domain.model.Rfq;
import com.eprocure.vendor.domain.repository.RfqFilter;
import com.eprocure.vendor.domain.repository.RfqRepository;
import com.eprocure.vendor.infrastructure.persistence.entity.RfqDbEntity;
import com.eprocure.vendor.infrastructure.persistence.entity.RfqInvitationDbEntity;
import com.eprocure.vendor.infrastructure.persistence.entity.RfqLineItemDbEntity;
import com.eprocure.vendor.infrastructure.persistence.mapper.RfqMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class RfqRepositoryImpl implements RfqRepository {
    private final RfqMapper rfqMapper;
    private final ObjectMapper objectMapper;

    public RfqRepositoryImpl(
            RfqMapper rfqMapper,
            @Qualifier("domainObjectMapper") ObjectMapper objectMapper) {
        this.rfqMapper = rfqMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public String nextRfqNumber() {
        return rfqMapper.nextRfqNumber();
    }

    @Override
    public Optional<Rfq> findById(UUID id) {
        return rfqMapper.findRfqById(id).map(this::hydrate);
    }

    @Override
    public Optional<Rfq> findByIdempotencyKey(UUID idempotencyKey) {
        return rfqMapper.findRfqByIdempotencyKey(idempotencyKey).map(this::hydrate);
    }

    @Override
    public List<Rfq> findByFilter(RfqFilter filter) {
        return rfqMapper.findByFilter(filter).stream()
                .map(this::hydrate)
                .toList();
    }

    @Override
    public long countByFilter(RfqFilter filter) {
        return rfqMapper.countByFilter(filter);
    }

    @Override
    public void save(Rfq rfq) {
        rfqMapper.insertRfq(RfqDbEntity.from(rfq));
        rfq.lineItems().stream()
                .map(item -> RfqLineItemDbEntity.from(rfq.id(), item, rfq.createdBy()))
                .forEach(rfqMapper::insertLineItem);
        rfq.invitations().stream()
                .map(invitation -> RfqInvitationDbEntity.from(rfq.id(), invitation, rfq.createdBy()))
                .forEach(rfqMapper::insertInvitation);
    }

    @Override
    public void updateStatus(Rfq rfq) {
        rfqMapper.updateStatus(RfqDbEntity.from(rfq));
    }

    private Rfq hydrate(RfqDbEntity entity) {
        entity.setLineItems(rfqMapper.findLineItemsByRfqId(entity.getId()));
        entity.setInvitations(rfqMapper.findInvitationsByRfqId(entity.getId()));
        return objectMapper.convertValue(entity, Rfq.class);
    }
}
