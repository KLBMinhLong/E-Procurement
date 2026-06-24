package com.eprocure.admin.infrastructure.persistence.repository;

import com.eprocure.admin.domain.model.AdminConfigAction;
import com.eprocure.admin.domain.model.AdminConfigActionStatus;
import com.eprocure.admin.domain.model.AdminConfigActionType;
import com.eprocure.admin.domain.repository.AdminConfigActionRepository;
import com.eprocure.admin.infrastructure.persistence.entity.AdminConfigActionDbEntity;
import com.eprocure.admin.infrastructure.persistence.mapper.AdminConfigActionMapper;
import java.util.Optional;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

@Repository
public class AdminConfigActionRepositoryImpl implements AdminConfigActionRepository {
    private static final Logger log = LogManager.getLogger(AdminConfigActionRepositoryImpl.class);

    private final AdminConfigActionMapper mapper;

    public AdminConfigActionRepositoryImpl(AdminConfigActionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<AdminConfigAction> findByIdempotencyKey(UUID createdBy, UUID idempotencyKey) {
        log.debug("[REPO] findByIdempotencyKey admin_config_actions | createdBy={}", createdBy);
        return mapper.findByIdempotencyKey(createdBy, idempotencyKey).map(this::toDomain);
    }

    @Override
    public AdminConfigAction save(AdminConfigAction action) {
        log.debug("[REPO] insert admin_config_actions | id={} | actionType={}", action.id(), action.actionType());
        mapper.insert(toEntity(action));
        return action;
    }

    private AdminConfigActionDbEntity toEntity(AdminConfigAction action) {
        AdminConfigActionDbEntity entity = new AdminConfigActionDbEntity();
        entity.setId(action.id());
        entity.setActionType(action.actionType().name());
        entity.setStatus(action.status().name());
        entity.setServiceName(action.serviceName().orElse(null));
        entity.setChangeReason(action.changeReason().orElse(null));
        entity.setVariableCount(action.variableCount());
        entity.setRequiresRestart(action.requiresRestart());
        entity.setEstimatedDowntimeSeconds(action.estimatedDowntimeSeconds().orElse(null));
        entity.setKeyVersion(action.keyVersion().orElse(null));
        entity.setIdempotencyKey(action.idempotencyKey());
        entity.setRequestedAt(action.requestedAt());
        entity.setCreatedBy(action.createdBy());
        return entity;
    }

    private AdminConfigAction toDomain(AdminConfigActionDbEntity entity) {
        return new AdminConfigAction(
                entity.getId(),
                AdminConfigActionType.valueOf(entity.getActionType()),
                AdminConfigActionStatus.valueOf(entity.getStatus()),
                Optional.ofNullable(entity.getServiceName()),
                Optional.ofNullable(entity.getChangeReason()),
                entity.getVariableCount(),
                entity.isRequiresRestart(),
                Optional.ofNullable(entity.getEstimatedDowntimeSeconds()),
                Optional.ofNullable(entity.getKeyVersion()),
                entity.getIdempotencyKey(),
                entity.getRequestedAt(),
                entity.getCreatedBy());
    }
}
