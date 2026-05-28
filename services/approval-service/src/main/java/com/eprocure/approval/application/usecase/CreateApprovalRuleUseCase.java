package com.eprocure.approval.application.usecase;

import com.eprocure.approval.application.port.in.CreateApprovalRuleCommand;
import com.eprocure.approval.application.service.ApprovalRuleAdminView;
import com.eprocure.approval.application.service.ApprovalRuleCommandAssembler;
import com.eprocure.approval.application.service.ApprovalRuleMutationResult;
import com.eprocure.approval.application.service.IdempotencyService;
import com.eprocure.approval.common.exception.BusinessException;
import com.eprocure.approval.common.exception.ErrorCode;
import com.eprocure.approval.common.util.LogMaskingUtil;
import com.eprocure.approval.domain.model.ApprovalRule;
import com.eprocure.approval.domain.repository.ApprovalRuleRepository;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CreateApprovalRuleUseCase {
    private static final Logger log = LogManager.getLogger(CreateApprovalRuleUseCase.class);
    private static final String IDEMPOTENCY_OPERATION = "approval-rule-create";

    private final ApprovalRuleRepository approvalRuleRepository;
    private final IdempotencyService idempotencyService;

    public CreateApprovalRuleUseCase(
            ApprovalRuleRepository approvalRuleRepository,
            IdempotencyService idempotencyService) {
        this.approvalRuleRepository = approvalRuleRepository;
        this.idempotencyService = idempotencyService;
    }

    @Transactional
    public ApprovalRuleMutationResult execute(CreateApprovalRuleCommand command, String idempotencyKey) {
        idempotencyService.verify(idempotencyKey);
        return idempotencyService.find(IDEMPOTENCY_OPERATION, command.actorId(), idempotencyKey, ApprovalRuleAdminView.class)
                .map(view -> new ApprovalRuleMutationResult(view, true))
                .orElseGet(() -> create(command, idempotencyKey));
    }

    private ApprovalRuleMutationResult create(CreateApprovalRuleCommand command, String idempotencyKey) {
        log.info("[ACTION] Start CreateApprovalRule | actorId={} | ruleName={}",
                LogMaskingUtil.maskId(command.actorId()),
                command.ruleName());
        if (approvalRuleRepository.existsByRuleName(command.ruleName(), null)) {
            throw new BusinessException(ErrorCode.APR_010);
        }
        ApprovalRule rule = ApprovalRule.create(
                UUID.randomUUID(),
                command.ruleName(),
                command.priority() == null ? 100 : command.priority(),
                command.ruleType(),
                ApprovalRuleCommandAssembler.toCondition(command.conditions()),
                ApprovalRuleCommandAssembler.toStepTemplates(command.steps()),
                command.description());
        approvalRuleRepository.save(rule, command.actorId());
        ApprovalRuleAdminView view = ApprovalRuleAdminView.from(rule);
        idempotencyService.save(IDEMPOTENCY_OPERATION, command.actorId(), idempotencyKey, view);
        log.info("[AUDIT] create_approval_rule | actor={} | entity=approval_rule/{} | result=SUCCESS",
                LogMaskingUtil.maskId(command.actorId()),
                rule.getId());
        return new ApprovalRuleMutationResult(view, false);
    }
}
