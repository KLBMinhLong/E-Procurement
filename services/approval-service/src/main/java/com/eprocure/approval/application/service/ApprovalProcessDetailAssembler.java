package com.eprocure.approval.application.service;

import com.eprocure.approval.application.port.out.UserResolverPort;
import com.eprocure.approval.domain.model.ApprovalProcess;
import com.eprocure.approval.domain.model.ApprovalStep;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ApprovalProcessDetailAssembler {
    private final UserResolverPort userResolverPort;

    public ApprovalProcessDetailAssembler(UserResolverPort userResolverPort) {
        this.userResolverPort = userResolverPort;
    }

    public ApprovalProcessDetail toDetail(ApprovalProcess process) {
        Map<UUID, UserResolverPort.UserSummary> userCache = new HashMap<>();
        List<ApprovalProcessDetail.StepDetail> steps = process.getSteps().stream()
                .map(step -> toStepDetail(step, userCache))
                .toList();

        return new ApprovalProcessDetail(
                process.getId(),
                process.getEntityType().name(),
                process.getEntityId(),
                process.getEntityNumber(),
                process.getStatus().name(),
                process.getCurrentStepIndex(),
                steps,
                process.getStartedAt(),
                process.getCompletedAt().orElse(null));
    }

    private ApprovalProcessDetail.StepDetail toStepDetail(
            ApprovalStep step,
            Map<UUID, UserResolverPort.UserSummary> userCache) {
        ApprovalProcessDetail.Approver approver = resolveApprover(step.getApproverId(), userCache);
        return new ApprovalProcessDetail.StepDetail(
                step.getStepIndex(),
                step.getRequiredPermission(),
                approver,
                step.getDelegateId().orElse(null),
                step.getStatus().name(),
                step.getAction().map(Enum::name).orElse(null),
                step.getComment().orElse(null),
                step.getSlaDeadline(),
                step.getAssignedAt(),
                step.getActedAt().orElse(null),
                step.isEscalated());
    }

    private ApprovalProcessDetail.Approver resolveApprover(
            UUID approverId,
            Map<UUID, UserResolverPort.UserSummary> userCache) {
        UserResolverPort.UserSummary summary = userCache.computeIfAbsent(
                approverId,
                id -> userResolverPort.getUserById(id).orElse(null));
        if (summary != null) {
            return new ApprovalProcessDetail.Approver(approverId, summary.fullName());
        }
        return new ApprovalProcessDetail.Approver(approverId, "User (" + approverId + ")");
    }
}
