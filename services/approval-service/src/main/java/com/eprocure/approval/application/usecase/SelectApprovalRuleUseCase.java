package com.eprocure.approval.application.usecase;

import com.eprocure.approval.application.port.in.SelectApprovalRuleQuery;
import com.eprocure.approval.application.service.SelectedApprovalRuleView;
import com.eprocure.approval.common.exception.BusinessException;
import com.eprocure.approval.common.exception.ErrorCode;
import com.eprocure.approval.domain.model.ApprovalRule;
import com.eprocure.approval.domain.model.ApprovalStepTemplate;
import com.eprocure.approval.domain.repository.ApprovalRuleRepository;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SelectApprovalRuleUseCase {
    private static final Logger log = LogManager.getLogger(SelectApprovalRuleUseCase.class);

    private final ApprovalRuleRepository approvalRuleRepository;

    public SelectApprovalRuleUseCase(ApprovalRuleRepository approvalRuleRepository) {
        this.approvalRuleRepository = approvalRuleRepository;
    }

    @Transactional(readOnly = true)
    public SelectedApprovalRuleView execute(SelectApprovalRuleQuery query) {
        log.info("[ACTION] Start SelectApprovalRule | prId={}", query.purchaseRequestId());

        List<ApprovalRule> matchingRules = approvalRuleRepository.findActiveRules().stream()
                .filter(rule -> rule.matches(
                        query.totalAmount(),
                        query.categories(),
                        query.departmentId(),
                        query.priority()))
                .sorted(ApprovalRule.PRIORITY_ORDER)
                .toList();

        ApprovalRule primaryRule = matchingRules.stream()
                .filter(rule -> !rule.isAdditive())
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.APR_001));

        List<ApprovalRule> appliedRules = new ArrayList<>();
        appliedRules.add(primaryRule);
        matchingRules.stream()
                .filter(ApprovalRule::isAdditive)
                .filter(rule -> !rule.getId().equals(primaryRule.getId()))
                .forEach(appliedRules::add);

        SelectedApprovalRuleView result = toView(primaryRule, appliedRules);
        log.info("[ACTION] Complete SelectApprovalRule | prId={} | primaryRule={} | stepCount={}",
                query.purchaseRequestId(),
                primaryRule.getRuleName(),
                result.steps().size());
        return result;
    }

    private SelectedApprovalRuleView toView(ApprovalRule primaryRule, List<ApprovalRule> appliedRules) {
        List<SelectedApprovalRuleView.StepView> steps = new ArrayList<>();
        int sequence = 1;
        for (ApprovalRule rule : appliedRules) {
            for (ApprovalStepTemplate template : rule.getStepTemplates()) {
                steps.add(new SelectedApprovalRuleView.StepView(
                        sequence++,
                        template.stepIndex(),
                        rule.getRuleName(),
                        template.approverRole(),
                        template.stepType(),
                        template.slaHours(),
                        template.required()));
            }
        }
        return new SelectedApprovalRuleView(
                primaryRule.getId(),
                primaryRule.getRuleName(),
                appliedRules.stream().map(ApprovalRule::getRuleName).toList(),
                steps);
    }
}
