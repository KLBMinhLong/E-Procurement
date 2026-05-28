package com.eprocure.approval.application.usecase;

import com.eprocure.approval.application.port.in.DeactivateApprovalRuleCommand;
import com.eprocure.approval.application.service.ApprovalRuleAdminView;
import com.eprocure.approval.application.service.ApprovalRuleMutationResult;
import com.eprocure.approval.application.service.IdempotencyService;
import com.eprocure.approval.common.exception.BusinessException;
import com.eprocure.approval.common.exception.ErrorCode;
import com.eprocure.approval.common.util.LogMaskingUtil;
import com.eprocure.approval.domain.model.ApprovalRule;
import com.eprocure.approval.domain.repository.ApprovalRuleRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DeactivateApprovalRuleUseCase {
    private static final Logger log = LogManager.getLogger(DeactivateApprovalRuleUseCase.class);
    private static final String IDEMPOTENCY_OPERATION_PREFIX = "approval-rule-deactivate:";

    private final ApprovalRuleRepository approvalRuleRepository;
    private final IdempotencyService idempotencyService;

    public DeactivateApprovalRuleUseCase(
            ApprovalRuleRepository approvalRuleRepository,
            IdempotencyService idempotencyService) {
        this.approvalRuleRepository = approvalRuleRepository;
        this.idempotencyService = idempotencyService;
    }

    @Transactional
    public ApprovalRuleMutationResult execute(DeactivateApprovalRuleCommand command, String idempotencyKey) {
        idempotencyService.verify(idempotencyKey);
        String operation = IDEMPOTENCY_OPERATION_PREFIX + command.id();
        return idempotencyService.find(operation, command.actorId(), idempotencyKey, ApprovalRuleAdminView.class)
                .map(view -> new ApprovalRuleMutationResult(view, true))
                .orElseGet(() -> deactivate(command, idempotencyKey, operation));
    }

    private ApprovalRuleMutationResult deactivate(DeactivateApprovalRuleCommand command, String idempotencyKey, String operation) {
        log.info("[ACTION] Start DeactivateApprovalRule | actorId={} | ruleId={}",
                LogMaskingUtil.maskId(command.actorId()),
                command.id());
        ApprovalRule existing = approvalRuleRepository.findById(command.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.APR_009));
        ApprovalRule deactivated = existing.deactivate();
        approvalRuleRepository.deactivate(command.id(), command.actorId());
        ApprovalRuleAdminView view = ApprovalRuleAdminView.from(deactivated);
        idempotencyService.save(operation, command.actorId(), idempotencyKey, view);
        log.info("[AUDIT] deactivate_approval_rule | actor={} | entity=approval_rule/{} | reason={} | result=SUCCESS",
                LogMaskingUtil.maskId(command.actorId()),
                command.id(),
                command.reason());
        return new ApprovalRuleMutationResult(view, false);
    }
}
