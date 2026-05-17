package com.eprocure.iam.application.usecase;

import com.eprocure.iam.application.service.DelegationDetailView;
import com.eprocure.iam.application.service.DelegationViewAssembler;
import com.eprocure.iam.common.util.LogMaskingUtil;
import com.eprocure.iam.domain.model.DelegationStatus;
import com.eprocure.iam.domain.repository.DelegationRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListMyDelegationsUseCase {
    private static final Logger log = LoggerFactory.getLogger(ListMyDelegationsUseCase.class);
    private final DelegationRepository delegationRepository;
    private final DelegationViewAssembler delegationViewAssembler;
    private final Clock clock;

    public ListMyDelegationsUseCase(
            DelegationRepository delegationRepository,
            DelegationViewAssembler delegationViewAssembler,
            Clock clock) {
        this.delegationRepository = delegationRepository;
        this.delegationViewAssembler = delegationViewAssembler;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<DelegationDetailView> execute(UUID delegatorId, DelegationStatus status) {
        log.info("[ACTION] Start ListMyDelegations | delegatorId={} | status={}",
                LogMaskingUtil.maskId(delegatorId),
                status);
        List<DelegationDetailView> views = delegationRepository.findByDelegatorId(delegatorId).stream()
                .filter(delegation -> status == null || delegation.effectiveStatus(clock.instant()) == status)
                .map(delegationViewAssembler::toView)
                .toList();
        log.info("[ACTION] Complete ListMyDelegations | delegatorId={} | count={}",
                LogMaskingUtil.maskId(delegatorId),
                views.size());
        return views;
    }
}
