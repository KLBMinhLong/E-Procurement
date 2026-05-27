package com.eprocure.approval.application.usecase;

import com.eprocure.approval.application.port.in.SelectApprovalRuleQuery;
import com.eprocure.approval.application.service.ApprovalRuleSelectionService;
import com.eprocure.approval.application.service.SelectedApprovalRuleView;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SelectApprovalRuleUseCase {
    private static final Logger log = LogManager.getLogger(SelectApprovalRuleUseCase.class);

    private final ApprovalRuleSelectionService approvalRuleSelectionService;

    public SelectApprovalRuleUseCase(ApprovalRuleSelectionService approvalRuleSelectionService) {
        this.approvalRuleSelectionService = approvalRuleSelectionService;
    }

    @Transactional(readOnly = true)
    public SelectedApprovalRuleView execute(SelectApprovalRuleQuery query) {
        log.info("[ACTION] Start SelectApprovalRule | prId={}", query.purchaseRequestId());

        SelectedApprovalRuleView result = approvalRuleSelectionService.select(query);
        log.info("[ACTION] Complete SelectApprovalRule | prId={} | primaryRule={} | stepCount={}",
                query.purchaseRequestId(),
                result.primaryRuleName(),
                result.steps().size());
        return result;
    }
}
