package com.eprocure.iam.application.usecase;

import com.eprocure.iam.application.port.in.ResolveActiveDelegationQuery;
import com.eprocure.iam.application.service.ActiveDelegationView;
import com.eprocure.iam.application.service.UserViewAssembler;
import com.eprocure.iam.common.util.LogMaskingUtil;
import com.eprocure.iam.domain.repository.DelegationRepository;
import com.eprocure.iam.domain.repository.UserRepository;
import java.time.Clock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResolveActiveDelegationUseCase {
    private static final Logger log = LogManager.getLogger(ResolveActiveDelegationUseCase.class);

    private final DelegationRepository delegationRepository;
    private final UserRepository userRepository;
    private final UserViewAssembler userViewAssembler;
    private final Clock clock;

    public ResolveActiveDelegationUseCase(
            DelegationRepository delegationRepository,
            UserRepository userRepository,
            UserViewAssembler userViewAssembler,
            Clock clock) {
        this.delegationRepository = delegationRepository;
        this.userRepository = userRepository;
        this.userViewAssembler = userViewAssembler;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ActiveDelegationView execute(ResolveActiveDelegationQuery query) {
        log.info("[ACTION] Start ResolveActiveDelegation | delegatorId={} | requesterId={}",
                LogMaskingUtil.maskId(query.delegatorId()),
                LogMaskingUtil.maskId(query.requesterId()));

        ActiveDelegationView result = delegationRepository.findActiveForApproval(
                        query.delegatorId(),
                        query.requesterDepartmentId(),
                        query.totalAmount(),
                        query.currency(),
                        query.categories(),
                        clock.instant())
                .flatMap(delegation -> userRepository.findById(delegation.getDelegateId())
                        .map(user -> ActiveDelegationView.active(
                                delegation.getId(),
                                delegation.getDelegatorId(),
                                delegation.getDelegateId(),
                                userViewAssembler.toSummary(user))))
                .orElseGet(ActiveDelegationView::inactive);

        log.info("[ACTION] Complete ResolveActiveDelegation | delegatorId={} | active={}",
                LogMaskingUtil.maskId(query.delegatorId()),
                result.active());
        return result;
    }
}
