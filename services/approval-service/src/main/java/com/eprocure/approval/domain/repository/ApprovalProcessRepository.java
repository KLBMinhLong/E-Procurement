package com.eprocure.approval.domain.repository;

import com.eprocure.approval.domain.model.ApprovalEntityType;
import com.eprocure.approval.domain.model.ApprovalProcess;
import java.util.UUID;

public interface ApprovalProcessRepository {
    boolean existsRunningByEntity(ApprovalEntityType entityType, UUID entityId);

    void save(ApprovalProcess process);
}
