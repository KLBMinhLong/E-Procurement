package com.eprocure.iam.application.service;

import com.eprocure.iam.common.exception.BusinessException;
import com.eprocure.iam.common.exception.ErrorCode;
import com.eprocure.iam.domain.model.Delegation;
import com.eprocure.iam.domain.repository.UserRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;

@Service
public class DelegationViewAssembler {
    private final UserRepository userRepository;
    private final UserViewAssembler userViewAssembler;
    private final Clock clock;

    public DelegationViewAssembler(UserRepository userRepository, UserViewAssembler userViewAssembler, Clock clock) {
        this.userRepository = userRepository;
        this.userViewAssembler = userViewAssembler;
        this.clock = clock;
    }

    public DelegationDetailView toView(Delegation delegation) {
        return new DelegationDetailView(
                delegation.getId(),
                userRepository.findById(delegation.getDelegatorId())
                        .map(userViewAssembler::toSummary)
                        .orElseThrow(() -> new BusinessException(ErrorCode.IAM_030)),
                userRepository.findById(delegation.getDelegateId())
                        .map(userViewAssembler::toSummary)
                        .orElseThrow(() -> new BusinessException(ErrorCode.IAM_030)),
                delegation.getStartAt(),
                delegation.getEndAt(),
                delegation.getMaxValue().map(value -> value.setScale(4).toPlainString()).orElse(null),
                delegation.getCurrency(),
                delegation.getAllowedCategories().orElse(null),
                delegation.getScope(),
                delegation.effectiveStatus(clock.instant()),
                delegation.getCreatedAt());
    }
}
