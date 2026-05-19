package com.eprocure.pr.infrastructure.persistence.repository;

import com.eprocure.pr.domain.model.PurchaseRequest;
import com.eprocure.pr.domain.repository.PurchaseRequestRepository;
import com.eprocure.pr.infrastructure.persistence.entity.PrLineItemDbEntity;
import com.eprocure.pr.infrastructure.persistence.entity.PurchaseRequestDbEntity;
import com.eprocure.pr.infrastructure.persistence.mapper.PurchaseRequestMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class PurchaseRequestRepositoryImpl implements PurchaseRequestRepository {
    private static final Logger log = LogManager.getLogger(PurchaseRequestRepositoryImpl.class);

    private final PurchaseRequestMapper mapper;
    private final ObjectMapper objectMapper;

    public PurchaseRequestRepositoryImpl(
            PurchaseRequestMapper mapper,
            @Qualifier("domainObjectMapper") ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(PurchaseRequest purchaseRequest) {
        log.debug("[REPO] insert purchase_requests | id={}", purchaseRequest.getId());
        PurchaseRequestDbEntity entity = objectMapper.convertValue(purchaseRequest, PurchaseRequestDbEntity.class);
        mapper.insert(entity);
        entity.getLineItems().forEach(lineItem -> mapper.insertLineItem(lineItem, purchaseRequest.getCreatedBy()));
    }

    @Override
    public void update(PurchaseRequest purchaseRequest) {
        throw new UnsupportedOperationException("PurchaseRequest update is not implemented in this slice");
    }

    @Override
    public Optional<PurchaseRequest> findById(UUID id) {
        log.debug("[REPO] findById purchase_requests | id={}", id);
        return mapper.findById(id).map(this::withLineItems).map(this::toDomain);
    }

    @Override
    public Optional<PurchaseRequest> findByPrNumber(String prNumber) {
        log.debug("[REPO] findByPrNumber purchase_requests | prNumber={}", prNumber);
        return mapper.findByPrNumber(prNumber).map(this::withLineItems).map(this::toDomain);
    }

    @Override
    public boolean existsByPrNumber(String prNumber) {
        return mapper.existsByPrNumber(prNumber);
    }

    @Override
    public void softDelete(UUID id, UUID deletedBy, Instant deletedAt) {
        log.debug("[REPO] softDelete purchase_requests | id={}", id);
        mapper.softDelete(id, deletedBy, deletedAt);
    }

    private PurchaseRequestDbEntity withLineItems(PurchaseRequestDbEntity entity) {
        entity.setLineItems(mapper.findLineItems(entity.getId()));
        return entity;
    }

    private PurchaseRequest toDomain(PurchaseRequestDbEntity entity) {
        return objectMapper.convertValue(entity, PurchaseRequest.class);
    }
}
