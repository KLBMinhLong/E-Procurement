package com.eprocure.approval.domain.repository;

import com.eprocure.approval.domain.model.ApprovalEntityType;
import com.eprocure.approval.domain.model.ApprovalProcess;
import com.eprocure.approval.domain.model.ApprovalStep;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApprovalProcessRepository {
    boolean existsRunningByEntity(ApprovalEntityType entityType, UUID entityId);

    void save(ApprovalProcess process);

    Optional<ApprovalProcess> findRunningByStepId(UUID stepId);

    Optional<ApprovalProcess> findRunningByCamundaTaskId(String camundaTaskId);

    List<ApprovalProcess> findRunningProcessesWithOverdueSteps(Instant now, int limit);

    void updateProcessRuntime(ApprovalProcess process);

    void updateStep(ApprovalStep step);

    void insertStep(ApprovalStep step);

    List<PendingTaskProjection> findPendingTasks(
            UUID userId,
            String priority,
            String entityType,
            java.math.BigDecimal minAmount,
            Boolean isOverdue,
            String orderByColumn,
            String sortDirection,
            int offset,
            int limit);

    long countPendingTasks(
            UUID userId,
            String priority,
            String entityType,
            java.math.BigDecimal minAmount,
            Boolean isOverdue);
}
