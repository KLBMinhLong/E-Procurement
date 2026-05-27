package com.eprocure.approval.application.service;

import com.eprocure.approval.application.port.in.ResolveApprovalChainCommand;
import com.eprocure.approval.application.port.in.SelectApprovalRuleQuery;
import com.eprocure.approval.application.port.out.OrgApproverPort;
import com.eprocure.approval.application.port.out.OrgApproverPort.ApproverCandidate;
import com.eprocure.approval.application.port.out.OrgApproverPort.ResolveApproverQuery;
import com.eprocure.approval.application.service.ResolvedApprovalChainView.ApproverView;
import com.eprocure.approval.application.service.ResolvedApprovalChainView.ResolvedApprovalStepView;
import com.eprocure.approval.common.exception.BusinessException;
import com.eprocure.approval.common.exception.ErrorCode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ApprovalChainResolutionService {
    private final ApprovalRuleSelectionService approvalRuleSelectionService;
    private final OrgApproverPort orgApproverPort;
    private final SlaDeadlineCalculator slaDeadlineCalculator;

    public ApprovalChainResolutionService(
            ApprovalRuleSelectionService approvalRuleSelectionService,
            OrgApproverPort orgApproverPort,
            SlaDeadlineCalculator slaDeadlineCalculator) {
        this.approvalRuleSelectionService = approvalRuleSelectionService;
        this.orgApproverPort = orgApproverPort;
        this.slaDeadlineCalculator = slaDeadlineCalculator;
    }

    public ResolvedApprovalChainView resolve(ResolveApprovalChainCommand command) {
        SelectedApprovalRuleView selectedRule = approvalRuleSelectionService.select(new SelectApprovalRuleQuery(
                command.purchaseRequestId(),
                command.departmentId(),
                command.totalAmount(),
                command.categories(),
                command.priority()));

        List<ResolvedApprovalStepView> steps = new ArrayList<>();
        Instant assignedAt = slaDeadlineCalculator.now();
        for (SelectedApprovalRuleView.StepView step : selectedRule.steps()) {
            ApproverCandidate approver = resolveStepApprover(step, command.departmentId(), command.requesterId());
            steps.add(new ResolvedApprovalStepView(
                    step.sequence(),
                    step.sourceStepIndex(),
                    step.sourceRuleName(),
                    step.approverRole(),
                    step.stepType(),
                    step.slaHours(),
                    slaDeadlineCalculator.calculateDeadline(assignedAt, step.slaHours(), command.priority()),
                    step.required(),
                    toView(approver)));
        }

        return new ResolvedApprovalChainView(
                command.purchaseRequestId(),
                selectedRule.primaryRuleId(),
                selectedRule.primaryRuleName(),
                selectedRule.appliedRuleNames(),
                steps);
    }

    private ApproverCandidate resolveStepApprover(
            SelectedApprovalRuleView.StepView step,
            UUID departmentId,
            UUID requesterId) {
        List<ApproverCandidate> candidates = orgApproverPort.resolveApprovers(new ResolveApproverQuery(
                step.approverRole(),
                departmentId,
                requesterId));
        if (candidates.isEmpty()) {
            throw new BusinessException(ErrorCode.APR_002);
        }

        boolean onlyRequesterCandidates = candidates.stream().allMatch(candidate -> requesterId.equals(candidate.id()));
        if (onlyRequesterCandidates) {
            throw new BusinessException(ErrorCode.APR_001);
        }

        return candidates.stream()
                .filter(candidate -> !requesterId.equals(candidate.id()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.APR_001));
    }

    private ApproverView toView(ApproverCandidate candidate) {
        return new ApproverView(
                candidate.id(),
                candidate.employeeCode(),
                candidate.username(),
                candidate.fullName(),
                candidate.email(),
                candidate.departmentId());
    }
}
