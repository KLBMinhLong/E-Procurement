package com.eprocure.approval.application.usecase;

import com.eprocure.approval.application.service.ApprovalProcessDetail;
import com.eprocure.approval.application.service.ApprovalProcessDetailAssembler;
import com.eprocure.approval.common.exception.BusinessException;
import com.eprocure.approval.common.exception.ErrorCode;
import com.eprocure.approval.common.util.LogMaskingUtil;
import com.eprocure.approval.domain.model.ApprovalEntityType;
import com.eprocure.approval.domain.model.ApprovalProcess;
import com.eprocure.approval.domain.model.ApprovalStep;
import com.eprocure.approval.domain.repository.ApprovalProcessRepository;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetApprovalProcessUseCase {
    private static final Logger log = LogManager.getLogger(GetApprovalProcessUseCase.class);
    private static final Set<String> APPROVAL_PERMISSIONS = Set.of(
            "PR_APPROVE_L1",
            "PR_APPROVE_L2",
            "PR_APPROVE_L3",
            "PR_APPROVE_FINANCE",
            "PR_APPROVE_EMERGENCY");

    private final ApprovalProcessRepository approvalProcessRepository;
    private final ApprovalProcessDetailAssembler processDetailAssembler;

    public GetApprovalProcessUseCase(
            ApprovalProcessRepository approvalProcessRepository,
            ApprovalProcessDetailAssembler processDetailAssembler) {
        this.approvalProcessRepository = approvalProcessRepository;
        this.processDetailAssembler = processDetailAssembler;
    }

    @Transactional(readOnly = true)
    public ApprovalProcessDetail execute(
            String entityTypeValue,
            UUID entityId,
            UUID actorId,
            UUID actorDepartmentId,
            Set<String> actorPermissions) {
        ApprovalEntityType entityType = parseEntityType(entityTypeValue);
        log.info("[ACTION] Start GetApprovalProcess | userId={} | entityType={} | entityId={}",
                LogMaskingUtil.maskId(actorId),
                entityType,
                entityId);

        ApprovalProcess process = approvalProcessRepository.findLatestByEntity(entityType, entityId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APR_011));
        if (!canView(process, actorId, actorDepartmentId, normalize(actorPermissions))) {
            throw new BusinessException(ErrorCode.IAM_004);
        }

        log.info("[ACTION] Complete GetApprovalProcess | processId={} | entityId={}",
                process.getId(),
                entityId);
        return processDetailAssembler.toDetail(process);
    }

    private ApprovalEntityType parseEntityType(String value) {
        try {
            return ApprovalEntityType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.APR_011);
        }
    }

    private Set<String> normalize(Set<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return Set.of();
        }
        return permissions.stream()
                .map(permission -> permission.trim().toUpperCase(Locale.ROOT))
                .filter(permission -> !permission.isBlank())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private boolean canView(
            ApprovalProcess process,
            UUID actorId,
            UUID actorDepartmentId,
            Set<String> permissions) {
        if (permissions.contains("PR_VIEW_ALL")) {
            return true;
        }
        if (process.getRequesterId().equals(actorId) && permissions.contains("PR_VIEW_OWN")) {
            return true;
        }
        if (process.getRequesterDepartmentId().equals(actorDepartmentId) && permissions.contains("PR_VIEW_DEPARTMENT")) {
            return true;
        }
        boolean hasApprovalPermission = permissions.stream().anyMatch(APPROVAL_PERMISSIONS::contains);
        if (!hasApprovalPermission) {
            return false;
        }
        return process.getSteps().stream().anyMatch(step -> isAssigned(step, actorId));
    }

    private boolean isAssigned(ApprovalStep step, UUID actorId) {
        return step.getApproverId().equals(actorId) ||
                step.getDelegateId().filter(actorId::equals).isPresent();
    }
}
