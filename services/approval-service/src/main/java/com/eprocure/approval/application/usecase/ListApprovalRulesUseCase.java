package com.eprocure.approval.application.usecase;

import com.eprocure.approval.application.service.ApprovalRuleAdminView;
import com.eprocure.approval.domain.repository.ApprovalRuleRepository;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ListApprovalRulesUseCase {
    private static final Logger log = LogManager.getLogger(ListApprovalRulesUseCase.class);

    private final ApprovalRuleRepository approvalRuleRepository;

    public ListApprovalRulesUseCase(ApprovalRuleRepository approvalRuleRepository) {
        this.approvalRuleRepository = approvalRuleRepository;
    }

    @Transactional(readOnly = true)
    public List<ApprovalRuleAdminView> execute() {
        log.info("[ACTION] Start ListApprovalRules");
        List<ApprovalRuleAdminView> result = approvalRuleRepository.findAllRules().stream()
                .map(ApprovalRuleAdminView::from)
                .toList();
        log.info("[ACTION] Complete ListApprovalRules | count={}", result.size());
        return result;
    }
}
