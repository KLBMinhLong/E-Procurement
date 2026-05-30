package com.eprocure.finance.application.usecase;

import com.eprocure.finance.application.port.in.RecordBudgetCommitmentCommand;
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

    public TentativeCommitBudgetUseCase(BudgetRepository budgetRepository) {
        this.budgetRepository = budgetRepository;
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

        if (!budgetRepository.existsTransaction(
                budget.id(), BudgetTransactionType.COMMIT_TENTATIVE, REFERENCE_TYPE, command.purchaseRequestId())) {
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
        }
        budgetRepository.markEventProcessed(
                command.eventId(), command.topic(), command.partitionId(), command.offsetValue(), HANDLER_NAME);
        log.info("[ACTION] Complete TentativeCommitBudget | prId={} | budgetId={}",
                LogMaskingUtil.maskId(command.purchaseRequestId()),
                LogMaskingUtil.maskId(budget.id()));
    }
}
