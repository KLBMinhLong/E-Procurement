package com.eprocure.approval.application.usecase;

import com.eprocure.approval.application.port.in.UpdateApprovalRuleCommand;
import com.eprocure.approval.application.service.ApprovalRuleAdminView;
import com.eprocure.approval.application.service.ApprovalRuleCommandAssembler;
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
public class UpdateApprovalRuleUseCase {
    private static final Logger log = LogManager.getLogger(UpdateApprovalRuleUseCase.class);
    private static final String IDEMPOTENCY_OPERATION_PREFIX = "approval-rule-update:";

    private final ApprovalRuleRepository approvalRuleRepository;
    private final IdempotencyService idempotencyService;

    public UpdateApprovalRuleUseCase(
            ApprovalRuleRepository approvalRuleRepository,
            IdempotencyService idempotencyService) {
        this.approvalRuleRepository = approvalRuleRepository;
        this.idempotencyService = idempotencyService;
    }

    @Transactional
    public ApprovalRuleMutationResult execute(UpdateApprovalRuleCommand command, String idempotencyKey) {
        idempotencyService.verify(idempotencyKey);
        String operation = IDEMPOTENCY_OPERATION_PREFIX + command.id();
        return idempotencyService.find(operation, command.actorId(), idempotencyKey, ApprovalRuleAdminView.class)
                .map(view -> new ApprovalRuleMutationResult(view, true))
                .orElseGet(() -> update(command, idempotencyKey, operation));
    }

    private ApprovalRuleMutationResult update(UpdateApprovalRuleCommand command, String idempotencyKey, String operation) {
        log.info("[ACTION] Start UpdateApprovalRule | actorId={} | ruleId={}",
                LogMaskingUtil.maskId(command.actorId()),
                command.id());
        ApprovalRule existing = approvalRuleRepository.findById(command.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.APR_009));
        if (approvalRuleRepository.existsByRuleName(command.ruleName(), command.id())) {
            throw new BusinessException(ErrorCode.APR_010);
        }
        ApprovalRule updated = existing.update(
                command.ruleName(),
                command.priority() == null ? existing.getPriority() : command.priority(),
                command.active() == null ? existing.isActive() : command.active(),
                command.ruleType(),
                ApprovalRuleCommandAssembler.toCondition(command.conditions()),
                ApprovalRuleCommandAssembler.toStepTemplates(command.steps()),
                command.description());
        approvalRuleRepository.update(updated, command.actorId());
        ApprovalRuleAdminView view = ApprovalRuleAdminView.from(updated);
        idempotencyService.save(operation, command.actorId(), idempotencyKey, view);
        log.info("[AUDIT] update_approval_rule | actor={} | entity=approval_rule/{} | result=SUCCESS",
                LogMaskingUtil.maskId(command.actorId()),
                updated.getId());
        return new ApprovalRuleMutationResult(view, false);
    }
}
