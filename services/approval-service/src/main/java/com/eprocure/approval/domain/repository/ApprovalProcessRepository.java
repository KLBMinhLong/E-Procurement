package com.eprocure.approval.domain.repository;

import com.eprocure.approval.domain.model.ApprovalEntityType;
import com.eprocure.approval.domain.model.ApprovalProcess;
import com.eprocure.approval.domain.model.ApprovalStep;
import java.util.Optional;
import java.util.UUID;

public interface ApprovalProcessRepository {
    boolean existsRunningByEntity(ApprovalEntityType entityType, UUID entityId);

    void save(ApprovalProcess process);

    Optional<ApprovalProcess> findRunningByStepId(UUID stepId);

    Optional<ApprovalProcess> findRunningByCamundaTaskId(String camundaTaskId);

    void updateProcessRuntime(ApprovalProcess process);

    void updateStep(ApprovalStep step);

    void insertStep(ApprovalStep step);
}
