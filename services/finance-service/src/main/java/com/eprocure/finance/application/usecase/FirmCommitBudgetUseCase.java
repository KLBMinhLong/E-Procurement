package com.eprocure.finance.application.usecase;

import com.eprocure.finance.application.port.in.RecordBudgetCommitmentCommand;
import com.eprocure.finance.common.exception.BusinessException;
import com.eprocure.finance.common.exception.ErrorCode;
import com.eprocure.finance.common.util.LogMaskingUtil;
import com.eprocure.finance.domain.model.BudgetCheckCriteria;
import com.eprocure.finance.domain.model.BudgetCommitmentHold;
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
public class FirmCommitBudgetUseCase {
    private static final Logger log = LogManager.getLogger(FirmCommitBudgetUseCase.class);
    private static final String HANDLER_NAME = "FirmCommitBudgetUseCase";
    private static final String REFERENCE_TYPE = "PURCHASE_REQUEST";
    private static final UUID SYSTEM_ACTOR_ID = UUID.fromString("00000000-0000-4000-8000-000000000010");

    private final BudgetRepository budgetRepository;

    public FirmCommitBudgetUseCase(BudgetRepository budgetRepository) {
        this.budgetRepository = budgetRepository;
    }

    @Transactional
    public void execute(RecordBudgetCommitmentCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        if (budgetRepository.existsProcessedEvent(command.eventId())) {
            log.info("[ACTION] Skip FirmCommitBudget duplicate event | eventId={}", command.eventId());
            return;
        }

        BudgetCommitmentHold hold = budgetRepository.findHeldCommitment(REFERENCE_TYPE, command.purchaseRequestId())
                .orElse(null);
        UUID budgetId = hold == null ? activeBudgetId(command) : hold.budgetId();
        if (hold != null && hold.hasHeldAmount() && !budgetRepository.existsTransaction(
                hold.budgetId(), BudgetTransactionType.RELEASE, REFERENCE_TYPE, command.purchaseRequestId())) {
            budgetRepository.insertTransaction(new BudgetTransaction(
                    hold.budgetId(),
                    BudgetTransactionType.RELEASE,
                    hold.amount(),
                    REFERENCE_TYPE,
                    command.purchaseRequestId(),
                    "Release tentative commit for " + command.prNumber(),
                    SYSTEM_ACTOR_ID,
                    command.occurredAt(),
                    command.eventId()));
        }
        if (!budgetRepository.existsTransaction(
                budgetId, BudgetTransactionType.COMMIT_FIRM, REFERENCE_TYPE, command.purchaseRequestId())) {
            budgetRepository.insertTransaction(new BudgetTransaction(
                    budgetId,
                    BudgetTransactionType.COMMIT_FIRM,
                    command.amount(),
                    REFERENCE_TYPE,
                    command.purchaseRequestId(),
                    "Firm commit for " + command.prNumber(),
                    SYSTEM_ACTOR_ID,
                    command.occurredAt(),
                    command.eventId()));
        }
        budgetRepository.markEventProcessed(
                command.eventId(), command.topic(), command.partitionId(), command.offsetValue(), HANDLER_NAME);
        log.info("[ACTION] Complete FirmCommitBudget | prId={} | budgetId={}",
                LogMaskingUtil.maskId(command.purchaseRequestId()),
                LogMaskingUtil.maskId(budgetId));
    }

    private UUID activeBudgetId(RecordBudgetCommitmentCommand command) {
        BudgetLedgerSummary budget = budgetRepository.findActiveSummary(new BudgetCheckCriteria(
                        command.departmentId(),
                        command.fiscalYear(),
                        command.glAccountCode()))
                .orElseThrow(() -> new BusinessException(ErrorCode.FIN_001));
        return budget.id();
    }
}
