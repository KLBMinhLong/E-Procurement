package com.eprocure.approval.infrastructure.persistence.repository;

import com.eprocure.approval.domain.model.ApprovalEntityType;
import com.eprocure.approval.domain.model.ApprovalAction;
import com.eprocure.approval.domain.model.ApprovalProcess;
import com.eprocure.approval.domain.model.ApprovalProcessStatus;
import com.eprocure.approval.domain.model.ApprovalStep;
import com.eprocure.approval.domain.model.ApprovalStepStatus;
import com.eprocure.approval.domain.model.ApprovalStepType;
import com.eprocure.approval.domain.model.PurchaseRequestPriority;
import com.eprocure.approval.domain.model.vo.Money;
import com.eprocure.approval.domain.repository.ApprovalProcessRepository;
import com.eprocure.approval.infrastructure.persistence.entity.ApprovalProcessDbEntity;
import com.eprocure.approval.infrastructure.persistence.entity.ApprovalStepDbEntity;
import com.eprocure.approval.infrastructure.persistence.mapper.ApprovalProcessMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

@Repository
public class ApprovalProcessRepositoryImpl implements ApprovalProcessRepository {
    private static final Logger log = LogManager.getLogger(ApprovalProcessRepositoryImpl.class);

    private final ApprovalProcessMapper mapper;
    private final ObjectMapper objectMapper;

    public ApprovalProcessRepositoryImpl(ApprovalProcessMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean existsRunningByEntity(ApprovalEntityType entityType, UUID entityId) {
        log.debug("[REPO] existsRunning approval_processes | entityType={} | entityId={}", entityType, entityId);
        return mapper.existsRunningByEntity(entityType.name(), entityId);
    }

    @Override
    public void save(ApprovalProcess process) {
        log.debug("[REPO] insert approval_processes | processId={}", process.getId());
        mapper.insertProcess(toEntity(process));
        process.getSteps().stream()
                .map(this::toEntity)
                .forEach(mapper::insertStep);
    }

    @Override
    public Optional<ApprovalProcess> findRunningByStepId(UUID stepId) {
        log.debug("[REPO] findRunningByStepId approval_processes | stepId={}", stepId);
        return Optional.ofNullable(mapper.findRunningByStepId(stepId))
                .map(this::toDomainWithSteps);
    }

    @Override
    public Optional<ApprovalProcess> findRunningByCamundaTaskId(String camundaTaskId) {
        log.debug("[REPO] findRunningByCamundaTaskId approval_processes | camundaTaskId={}", camundaTaskId);
        return Optional.ofNullable(mapper.findRunningByCamundaTaskId(camundaTaskId))
                .map(this::toDomainWithSteps);
    }

    @Override
    public void updateProcessRuntime(ApprovalProcess process) {
        log.debug("[REPO] updateRuntime approval_processes | processId={}", process.getId());
        mapper.updateProcessRuntime(toEntity(process));
    }

    @Override
    public void updateStep(ApprovalStep step) {
        log.debug("[REPO] update approval_steps | stepId={}", step.getId());
        mapper.updateStep(toEntity(step));
    }

    @Override
    public void insertStep(ApprovalStep step) {
        log.debug("[REPO] insert approval_steps | stepId={}", step.getId());
        mapper.insertStep(toEntity(step));
    }

    private ApprovalProcessDbEntity toEntity(ApprovalProcess process) {
        ApprovalProcessDbEntity entity = new ApprovalProcessDbEntity();
        entity.setId(process.getId());
        entity.setEntityType(process.getEntityType().name());
        entity.setEntityId(process.getEntityId());
        entity.setEntityNumber(process.getEntityNumber());
        entity.setEntityTitle(process.getEntityTitle());
        entity.setRequesterId(process.getRequesterId());
        entity.setRequesterDepartmentId(process.getRequesterDepartmentId());
        entity.setTotalAmount(process.getTotalAmount().amount());
        entity.setCurrency(process.getTotalAmount().currency());
        entity.setPriority(process.getPriority().name());
        entity.setCamundaProcessInstanceId(process.getCamundaProcessInstanceId());
        entity.setStatus(process.getStatus().name());
        entity.setCurrentStepIndex(process.getCurrentStepIndex());
        entity.setEntitySnapshotJson(toJson(process.getEntitySnapshot()));
        entity.setStartedAt(process.getStartedAt());
        entity.setCompletedAt(process.getCompletedAt().orElse(null));
        entity.setCreatedBy(process.getRequesterId());
        return entity;
    }

    private ApprovalStepDbEntity toEntity(ApprovalStep step) {
        ApprovalStepDbEntity entity = new ApprovalStepDbEntity();
        entity.setId(step.getId());
        entity.setProcessId(step.getProcessId());
        entity.setStepIndex(step.getStepIndex());
        entity.setStepType(step.getStepType().name());
        entity.setApproverRole(step.getApproverRole());
        entity.setApproverId(step.getApproverId());
        entity.setDelegateId(step.getDelegateId().orElse(null));
        entity.setStatus(step.getStatus().name());
        entity.setAction(step.getAction().map(ApprovalAction::name).orElse(null));
        entity.setComment(step.getComment().orElse(null));
        entity.setSlaDeadline(step.getSlaDeadline());
        entity.setAssignedAt(step.getAssignedAt());
        entity.setActedAt(step.getActedAt().orElse(null));
        entity.setCamundaTaskId(step.getCamundaTaskId().orElse(null));
        entity.setCreatedBy(step.getApproverId());
        return entity;
    }

    private ApprovalProcess toDomainWithSteps(ApprovalProcessDbEntity entity) {
        return ApprovalProcess.restore(
                entity.getId(),
                ApprovalEntityType.valueOf(entity.getEntityType()),
                entity.getEntityId(),
                entity.getEntityNumber(),
                entity.getEntityTitle(),
                entity.getRequesterId(),
                entity.getRequesterDepartmentId(),
                new Money(entity.getTotalAmount(), entity.getCurrency()),
                PurchaseRequestPriority.valueOf(entity.getPriority()),
                entity.getCamundaProcessInstanceId(),
                ApprovalProcessStatus.valueOf(entity.getStatus()),
                entity.getCurrentStepIndex(),
                fromJson(entity.getEntitySnapshotJson()),
                entity.getStartedAt(),
                entity.getCompletedAt(),
                mapper.findStepsByProcessId(entity.getId()).stream()
                        .map(this::toDomain)
                        .toList());
    }

    private ApprovalStep toDomain(ApprovalStepDbEntity entity) {
        return ApprovalStep.restore(
                entity.getId(),
                entity.getProcessId(),
                entity.getStepIndex(),
                ApprovalStepType.valueOf(entity.getStepType()),
                entity.getApproverRole(),
                entity.getApproverId(),
                entity.getDelegateId(),
                ApprovalStepStatus.valueOf(entity.getStatus()),
                entity.getAction() == null ? null : ApprovalAction.valueOf(entity.getAction()),
                entity.getComment(),
                entity.getSlaDeadline(),
                entity.getAssignedAt(),
                entity.getActedAt(),
                entity.getCamundaTaskId());
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize approval process snapshot", exception);
        }
    }

    private Map<String, Object> fromJson(String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot deserialize approval process snapshot", exception);
        }
    }

    @Override
    public java.util.List<com.eprocure.approval.domain.repository.PendingTaskProjection> findPendingTasks(
            UUID userId,
            String priority,
            String entityType,
            java.math.BigDecimal minAmount,
            Boolean isOverdue,
            String orderByColumn,
            String sortDirection,
            int offset,
            int limit) {
        log.debug("[REPO] findPendingTasks approval_processes | userId={} | priority={} | entityType={} | minAmount={} | isOverdue={}",
                userId, priority, entityType, minAmount, isOverdue);
        return mapper.findPendingTasks(userId, priority, entityType, minAmount, isOverdue, orderByColumn, sortDirection, offset, limit);
    }

    @Override
    public long countPendingTasks(
            UUID userId,
            String priority,
            String entityType,
            java.math.BigDecimal minAmount,
            Boolean isOverdue) {
        log.debug("[REPO] countPendingTasks approval_processes | userId={} | priority={} | entityType={} | minAmount={} | isOverdue={}",
                userId, priority, entityType, minAmount, isOverdue);
        return mapper.countPendingTasks(userId, priority, entityType, minAmount, isOverdue);
    }
}
