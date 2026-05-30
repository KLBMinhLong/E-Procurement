package com.eprocure.finance.application.usecase;

import com.eprocure.finance.application.port.in.RecordBudgetCommitmentCommand;
import com.eprocure.finance.application.port.out.BudgetDashboardCachePort;
import com.eprocure.finance.application.service.BudgetAlertService;
import com.eprocure.finance.common.exception.BusinessException;
import com.eprocure.finance.common.exception.ErrorCode;
import com.eprocure.finance.common.util.LogMaskingUtil;
import com.eprocure.finance.domain.model.BudgetCheckCriteria;
import com.eprocure.finance.domain.model.BudgetLedgerSummary;
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
public class TentativeCommitBudgetUseCase {
    private static final Logger log = LogManager.getLogger(TentativeCommitBudgetUseCase.class);
    private static final String HANDLER_NAME = "TentativeCommitBudgetUseCase";
    private static final String REFERENCE_TYPE = "PURCHASE_REQUEST";
    private static final UUID SYSTEM_ACTOR_ID = UUID.fromString("00000000-0000-4000-8000-000000000010");

    private final BudgetRepository budgetRepository;
    private final BudgetDashboardCachePort cachePort;
    private final BudgetAlertService budgetAlertService;

    public TentativeCommitBudgetUseCase(
            BudgetRepository budgetRepository,
            BudgetDashboardCachePort cachePort,
            BudgetAlertService budgetAlertService) {
        this.budgetRepository = budgetRepository;
        this.cachePort = cachePort;
        this.budgetAlertService = budgetAlertService;
    }

    @Transactional
    public void execute(RecordBudgetCommitmentCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        if (budgetRepository.existsProcessedEvent(command.eventId())) {
            log.info("[ACTION] Skip TentativeCommitBudget duplicate event | eventId={}", command.eventId());
            return;
        }
        BudgetLedgerSummary budget = budgetRepository.findActiveSummary(new BudgetCheckCriteria(
                        command.departmentId(),
                        command.fiscalYear(),
                        command.glAccountCode()))
                .orElseThrow(() -> new BusinessException(ErrorCode.FIN_001));

        boolean committed = !budgetRepository.existsTransaction(
                budget.id(), BudgetTransactionType.COMMIT_TENTATIVE, REFERENCE_TYPE, command.purchaseRequestId());
        if (committed) {
            budgetRepository.insertTransaction(new BudgetTransaction(
                    budget.id(),
                    BudgetTransactionType.COMMIT_TENTATIVE,
                    command.amount(),
                    REFERENCE_TYPE,
                    command.purchaseRequestId(),
                    "Tentative commit for " + command.prNumber(),
                    SYSTEM_ACTOR_ID,
                    command.occurredAt(),
                    command.eventId()));
            cachePort.evict(budget.id());
            publishAlert(budget.id(), command);
        }
        budgetRepository.markEventProcessed(
                command.eventId(), command.topic(), command.partitionId(), command.offsetValue(), HANDLER_NAME);
        log.info("[ACTION] Complete TentativeCommitBudget | prId={} | budgetId={}",
                LogMaskingUtil.maskId(command.purchaseRequestId()),
                LogMaskingUtil.maskId(budget.id()));
    }

    private void publishAlert(UUID budgetId, RecordBudgetCommitmentCommand command) {
        budgetRepository.findSummaryById(budgetId)
                .ifPresent(summary -> budgetAlertService.publishIfNeeded(
                        summary,
                        command.amount(),
                        REFERENCE_TYPE,
                        command.purchaseRequestId(),
                        command.eventId(),
                        "Budget is below policy threshold after tentative commitment"));
    }
}
