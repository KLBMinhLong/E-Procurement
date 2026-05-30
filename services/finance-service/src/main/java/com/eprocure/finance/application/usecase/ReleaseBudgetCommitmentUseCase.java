package com.eprocure.finance.application.usecase;

import com.eprocure.finance.application.port.in.ReleaseBudgetCommitmentCommand;
import com.eprocure.finance.application.port.out.BudgetDashboardCachePort;
import com.eprocure.finance.common.util.LogMaskingUtil;
import com.eprocure.finance.domain.model.BudgetCommitmentHold;
import com.eprocure.finance.domain.model.BudgetTransaction;
import com.eprocure.finance.domain.model.BudgetTransactionType;
import com.eprocure.finance.domain.repository.BudgetRepository;
import java.util.Objects;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReleaseBudgetCommitmentUseCase {
    private static final Logger log = LogManager.getLogger(ReleaseBudgetCommitmentUseCase.class);
    private static final String HANDLER_NAME = "ReleaseBudgetCommitmentUseCase";
    private static final String REFERENCE_TYPE = "PURCHASE_REQUEST";
    private static final UUID SYSTEM_ACTOR_ID = UUID.fromString("00000000-0000-4000-8000-000000000010");

    private final BudgetRepository budgetRepository;
    private final BudgetDashboardCachePort cachePort;

    public ReleaseBudgetCommitmentUseCase(BudgetRepository budgetRepository, BudgetDashboardCachePort cachePort) {
        this.budgetRepository = budgetRepository;
        this.cachePort = cachePort;
    }

    @Transactional
    public void execute(ReleaseBudgetCommitmentCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        if (budgetRepository.existsProcessedEvent(command.eventId())) {
            log.info("[ACTION] Skip ReleaseBudgetCommitment duplicate event | eventId={}", command.eventId());
            return;
        }

        budgetRepository.findHeldCommitment(REFERENCE_TYPE, command.purchaseRequestId())
                .filter(BudgetCommitmentHold::hasHeldAmount)
                .filter(hold -> !budgetRepository.existsTransaction(
                        hold.budgetId(), BudgetTransactionType.RELEASE, REFERENCE_TYPE, command.purchaseRequestId()))
                .ifPresent(hold -> {
                    budgetRepository.insertTransaction(new BudgetTransaction(
                            hold.budgetId(),
                            BudgetTransactionType.RELEASE,
                            hold.amount(),
                            REFERENCE_TYPE,
                            command.purchaseRequestId(),
                            "Release commitment for " + command.prNumber(),
                            SYSTEM_ACTOR_ID,
                            command.occurredAt(),
                            command.eventId()));
                    cachePort.evict(hold.budgetId());
                });

        budgetRepository.markEventProcessed(
                command.eventId(), command.topic(), command.partitionId(), command.offsetValue(), HANDLER_NAME);
        log.info("[ACTION] Complete ReleaseBudgetCommitment | prId={}",
                LogMaskingUtil.maskId(command.purchaseRequestId()));
    }
}
