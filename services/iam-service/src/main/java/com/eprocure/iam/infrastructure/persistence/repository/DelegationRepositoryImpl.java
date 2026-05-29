package com.eprocure.iam.infrastructure.persistence.repository;

import com.eprocure.iam.domain.model.Delegation;
import com.eprocure.iam.domain.model.DelegationStatus;
import com.eprocure.iam.domain.repository.DelegationRepository;
import com.eprocure.iam.infrastructure.persistence.entity.DelegationDbEntity;
import com.eprocure.iam.infrastructure.persistence.mapper.DelegationMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class DelegationRepositoryImpl implements DelegationRepository {
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };
    private final DelegationMapper delegationMapper;
    private final ObjectMapper objectMapper;

    public DelegationRepositoryImpl(DelegationMapper delegationMapper, @Qualifier("domainObjectMapper") ObjectMapper objectMapper) {
        this.delegationMapper = delegationMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<Delegation> findById(UUID id) {
        return Optional.ofNullable(delegationMapper.findById(id)).map(this::toDomain);
    }

    @Override
    public List<Delegation> findByDelegatorId(UUID delegatorId) {
        return delegationMapper.findByDelegatorId(delegatorId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Delegation> findActiveForApproval(
            UUID delegatorId,
            UUID requesterDepartmentId,
            BigDecimal totalAmount,
            String currency,
            List<String> categories,
            Instant effectiveAt) {
        return Optional.ofNullable(delegationMapper.findActiveForApproval(
                        delegatorId,
                        requesterDepartmentId,
                        totalAmount,
                        currency,
                        categories == null ? List.of() : categories,
                        effectiveAt))
                .map(this::toDomain);
    }

    @Override
    public boolean hasActiveOverlap(UUID delegatorId, Instant startAt, Instant endAt) {
        return delegationMapper.hasActiveOverlap(delegatorId, startAt, endAt);
    }

    @Override
    public Optional<String> findOrgPathByUserId(UUID userId) {
        return Optional.ofNullable(delegationMapper.findOrgPathByUserId(userId));
    }

    @Override
    public void save(Delegation delegation, UUID actorId) {
        delegationMapper.insert(toEntity(delegation), actorId);
    }

    @Override
    public void updateStatus(UUID delegationId, DelegationStatus status, UUID actorId) {
        delegationMapper.updateStatus(delegationId, status, actorId);
    }

    private Delegation toDomain(DelegationDbEntity entity) {
        return Delegation.reconstitute(
                entity.id,
                entity.delegatorId,
                entity.delegateId,
                entity.startAt,
                entity.endAt,
                entity.maxValue,
                entity.currency,
                parseAllowedCategories(entity.allowedCategoriesJson),
                entity.scope,
                entity.status,
                entity.createdAt);
    }

    private DelegationDbEntity toEntity(Delegation delegation) {
        DelegationDbEntity entity = objectMapper.convertValue(delegation, DelegationDbEntity.class);
        entity.allowedCategoriesJson = toJson(delegation.getAllowedCategories().orElse(null));
        return entity;
    }

    private List<String> parseAllowedCategories(String json) {
        try {
            return json == null || json.isBlank() ? null : objectMapper.readValue(json, STRING_LIST);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to parse delegation allowed categories", exception);
        }
    }

    private String toJson(List<String> values) {
        try {
            return values == null ? null : objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize delegation allowed categories", exception);
        }
    }
}
