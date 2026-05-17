package com.eprocure.iam.application.usecase;

import com.eprocure.iam.application.port.in.RevokeDelegationCommand;
import com.eprocure.iam.application.service.IdempotencyGuard;
import com.eprocure.iam.common.exception.BusinessException;
import com.eprocure.iam.common.exception.ErrorCode;
import com.eprocure.iam.common.util.LogMaskingUtil;
import com.eprocure.iam.domain.model.Delegation;
import com.eprocure.iam.domain.model.DelegationStatus;
import com.eprocure.iam.domain.repository.DelegationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RevokeDelegationUseCase {
    private static final Logger log = LoggerFactory.getLogger(RevokeDelegationUseCase.class);
    private final DelegationRepository delegationRepository;
    private final IdempotencyGuard idempotencyGuard;

    public RevokeDelegationUseCase(DelegationRepository delegationRepository, IdempotencyGuard idempotencyGuard) {
        this.delegationRepository = delegationRepository;
        this.idempotencyGuard = idempotencyGuard;
    }

    @Transactional
    public void execute(RevokeDelegationCommand command, String idempotencyKey) {
        idempotencyGuard.verify(idempotencyKey);
        log.info("[ACTION] Start RevokeDelegation | actorId={} | delegationId={}",
                LogMaskingUtil.maskId(command.actorId()),
                LogMaskingUtil.maskId(command.delegationId()));
        Delegation delegation = delegationRepository.findById(command.delegationId())
                .orElseThrow(() -> new BusinessException(ErrorCode.IAM_035));
        if (!delegation.getDelegatorId().equals(command.actorId())) {
            throw new BusinessException(ErrorCode.IAM_004);
        }
        delegation.revoke();
        delegationRepository.updateStatus(delegation.getId(), DelegationStatus.REVOKED, command.actorId());
        log.info("[ACTION] Complete RevokeDelegation | actorId={} | delegationId={}",
                LogMaskingUtil.maskId(command.actorId()),
                LogMaskingUtil.maskId(command.delegationId()));
    }
}
