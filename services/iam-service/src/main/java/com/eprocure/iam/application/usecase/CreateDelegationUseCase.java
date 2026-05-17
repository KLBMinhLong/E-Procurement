package com.eprocure.iam.application.usecase;

import com.eprocure.iam.application.port.in.CreateDelegationCommand;
import com.eprocure.iam.application.service.DelegationDetailView;
import com.eprocure.iam.application.service.DelegationViewAssembler;
import com.eprocure.iam.application.service.IdempotencyGuard;
import com.eprocure.iam.common.exception.BusinessException;
import com.eprocure.iam.common.exception.ErrorCode;
import com.eprocure.iam.common.util.LogMaskingUtil;
import com.eprocure.iam.domain.model.Delegation;
import com.eprocure.iam.domain.model.User;
import com.eprocure.iam.domain.repository.DelegationRepository;
import com.eprocure.iam.domain.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.Set;
import java.util.UUID;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateDelegationUseCase {
    private static final Logger log = LogManager.getLogger(CreateDelegationUseCase.class);
    private final DelegationRepository delegationRepository;
    private final UserRepository userRepository;
    private final DelegationViewAssembler delegationViewAssembler;
    private final IdempotencyGuard idempotencyGuard;
    private final Clock clock;

    public CreateDelegationUseCase(
            DelegationRepository delegationRepository,
            UserRepository userRepository,
            DelegationViewAssembler delegationViewAssembler,
            IdempotencyGuard idempotencyGuard,
            Clock clock) {
        this.delegationRepository = delegationRepository;
        this.userRepository = userRepository;
        this.delegationViewAssembler = delegationViewAssembler;
        this.idempotencyGuard = idempotencyGuard;
        this.clock = clock;
    }

    @Transactional
    public DelegationDetailView execute(CreateDelegationCommand command, String idempotencyKey) {
        idempotencyGuard.verify(idempotencyKey);
        log.info("[ACTION] Start CreateDelegation | delegatorId={} | delegateId={}",
                LogMaskingUtil.maskId(command.delegatorId()),
                LogMaskingUtil.maskId(command.delegateId()));
        User delegator = userRepository.findById(command.delegatorId())
                .orElseThrow(() -> new BusinessException(ErrorCode.IAM_030));
        User delegate = userRepository.findById(command.delegateId())
                .orElseThrow(() -> new BusinessException(ErrorCode.IAM_030));
        validateUsers(delegator, delegate);
        validateAuthority(delegator.getId());
        validateOrgLevel(delegator.getId(), delegate.getId());
        validateAmount(command.maxValue());
        if (delegationRepository.hasActiveOverlap(command.delegatorId(), command.startAt(), command.endAt())) {
            throw new BusinessException(ErrorCode.IAM_022);
        }

        Delegation delegation = Delegation.create(
                UUID.randomUUID(),
                command.delegatorId(),
                command.delegateId(),
                command.startAt(),
                command.endAt(),
                command.maxValue(),
                command.currency(),
                command.allowedCategories(),
                command.scope(),
                clock.instant());
        delegationRepository.save(delegation, command.delegatorId());
        DelegationDetailView view = delegationRepository.findById(delegation.getId())
                .map(delegationViewAssembler::toView)
                .orElseThrow(() -> new BusinessException(ErrorCode.IAM_035));
        log.info("[ACTION] Complete CreateDelegation | delegatorId={} | delegationId={}",
                LogMaskingUtil.maskId(command.delegatorId()),
                LogMaskingUtil.maskId(delegation.getId()));
        return view;
    }

    private void validateUsers(User delegator, User delegate) {
        if (delegator.getId().equals(delegate.getId())) {
            throw new BusinessException(ErrorCode.IAM_005);
        }
        if (!delegator.canLogin() || !delegate.canLogin()) {
            throw new BusinessException(ErrorCode.IAM_002);
        }
    }

    private void validateAuthority(UUID delegatorId) {
        Set<String> permissions = userRepository.findPermissionCodesByUserId(delegatorId);
        if (!permissions.contains("DELEGATION_MANAGE")) {
            throw new BusinessException(ErrorCode.IAM_021);
        }
    }

    private void validateOrgLevel(UUID delegatorId, UUID delegateId) {
        String delegatorPath = delegationRepository.findOrgPathByUserId(delegatorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IAM_020));
        String delegatePath = delegationRepository.findOrgPathByUserId(delegateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IAM_020));
        if (depth(delegatePath) > depth(delegatorPath)) {
            throw new BusinessException(ErrorCode.IAM_020);
        }
    }

    private void validateAmount(BigDecimal maxValue) {
        if (maxValue != null && maxValue.scale() > 4) {
            throw new BusinessException(ErrorCode.IAM_005);
        }
    }

    private int depth(String path) {
        if (path == null || path.isBlank()) {
            return Integer.MAX_VALUE;
        }
        return (int) java.util.Arrays.stream(path.split("/"))
                .filter(segment -> !segment.isBlank())
                .count();
    }
}
