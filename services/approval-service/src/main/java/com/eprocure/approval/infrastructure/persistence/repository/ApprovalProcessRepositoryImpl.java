package com.eprocure.approval.infrastructure.persistence.repository;

import com.eprocure.approval.domain.model.ApprovalEntityType;
import com.eprocure.approval.domain.model.ApprovalProcess;
import com.eprocure.approval.domain.model.ApprovalStep;
import com.eprocure.approval.domain.repository.ApprovalProcessRepository;
import com.eprocure.approval.infrastructure.persistence.entity.ApprovalProcessDbEntity;
import com.eprocure.approval.infrastructure.persistence.entity.ApprovalStepDbEntity;
import com.eprocure.approval.infrastructure.persistence.mapper.ApprovalProcessMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        entity.setSlaDeadline(step.getSlaDeadline());
        entity.setAssignedAt(step.getAssignedAt());
        entity.setCamundaTaskId(step.getCamundaTaskId().orElse(null));
        entity.setCreatedBy(step.getApproverId());
        return entity;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize approval process snapshot", exception);
        }
    }
}
