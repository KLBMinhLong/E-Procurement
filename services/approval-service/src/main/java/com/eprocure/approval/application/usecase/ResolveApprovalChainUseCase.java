package com.eprocure.approval.application.usecase;

import com.eprocure.approval.application.port.in.ResolveApprovalChainCommand;
import com.eprocure.approval.application.port.in.SelectApprovalRuleQuery;
import com.eprocure.approval.application.port.out.OrgApproverPort;
import com.eprocure.approval.application.port.out.OrgApproverPort.ApproverCandidate;
import com.eprocure.approval.application.port.out.OrgApproverPort.ResolveApproverQuery;
import com.eprocure.approval.application.service.ResolvedApprovalChainView;
import com.eprocure.approval.application.service.ResolvedApprovalChainView.ApproverView;
import com.eprocure.approval.application.service.ResolvedApprovalChainView.ResolvedApprovalStepView;
import com.eprocure.approval.application.service.SelectedApprovalRuleView;
import com.eprocure.approval.common.exception.BusinessException;
import com.eprocure.approval.common.exception.ErrorCode;
import com.eprocure.approval.common.util.LogMaskingUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ResolveApprovalChainUseCase {
    private static final Logger log = LogManager.getLogger(ResolveApprovalChainUseCase.class);

    private final SelectApprovalRuleUseCase selectApprovalRuleUseCase;
    private final OrgApproverPort orgApproverPort;

    public ResolveApprovalChainUseCase(
            SelectApprovalRuleUseCase selectApprovalRuleUseCase,
            OrgApproverPort orgApproverPort) {
        this.selectApprovalRuleUseCase = selectApprovalRuleUseCase;
        this.orgApproverPort = orgApproverPort;
    }

    @Transactional(readOnly = true)
    public ResolvedApprovalChainView execute(ResolveApprovalChainCommand command) {
        log.info("[ACTION] Start ResolveApprovalChain | prId={} | requesterId={} | departmentId={}",
                LogMaskingUtil.maskId(command.purchaseRequestId()),
                LogMaskingUtil.maskId(command.requesterId()),
                LogMaskingUtil.maskId(command.departmentId()));

        SelectedApprovalRuleView selectedRule = selectApprovalRuleUseCase.execute(new SelectApprovalRuleQuery(
                command.purchaseRequestId(),
                command.departmentId(),
                command.totalAmount(),
                command.categories(),
                command.priority()));

        List<ResolvedApprovalStepView> steps = new ArrayList<>();
        for (SelectedApprovalRuleView.StepView step : selectedRule.steps()) {
            ApproverCandidate approver = resolveStepApprover(step, command.departmentId(), command.requesterId());
            steps.add(new ResolvedApprovalStepView(
                    step.sequence(),
                    step.sourceStepIndex(),
                    step.sourceRuleName(),
                    step.approverRole(),
                    step.stepType(),
                    step.slaHours(),
                    step.required(),
                    toView(approver)));
        }

        log.info("[ACTION] Complete ResolveApprovalChain | prId={} | primaryRule={} | stepCount={}",
                LogMaskingUtil.maskId(command.purchaseRequestId()),
                selectedRule.primaryRuleName(),
                steps.size());
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
            throw new BusinessException(ErrorCode.APR_004);
        }

        return candidates.stream()
                .filter(candidate -> !requesterId.equals(candidate.id()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.APR_004));
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
