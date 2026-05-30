package com.eprocure.finance.application.usecase;

import com.eprocure.finance.application.port.in.CheckBudgetCommand;
import com.eprocure.finance.application.service.BudgetAlertService;
import com.eprocure.finance.application.service.BudgetCheckView;
import com.eprocure.finance.common.exception.BusinessException;
import com.eprocure.finance.common.exception.ErrorCode;
import com.eprocure.finance.common.util.LogMaskingUtil;
import com.eprocure.finance.domain.model.BudgetCheckCriteria;
import com.eprocure.finance.domain.model.BudgetCheckStatus;
import com.eprocure.finance.domain.model.BudgetLedgerSummary;
import com.eprocure.finance.domain.model.vo.Money;
import com.eprocure.finance.domain.repository.BudgetRepository;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckBudgetUseCase {
    private static final Logger log = LogManager.getLogger(CheckBudgetUseCase.class);

    private final BudgetRepository budgetRepository;
    private final BudgetAlertService budgetAlertService;

    public CheckBudgetUseCase(BudgetRepository budgetRepository, BudgetAlertService budgetAlertService) {
        this.budgetRepository = budgetRepository;
        this.budgetAlertService = budgetAlertService;
    }

    @Transactional(readOnly = true)
    public BudgetCheckView execute(CheckBudgetCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        if (!command.requestAmount().isPositive()) {
            throw new BusinessException(ErrorCode.FIN_002);
        }
        log.info("[ACTION] Start CheckBudget | departmentId={} | fiscalYear={}",
                LogMaskingUtil.maskId(command.departmentId()),
                command.fiscalYear());

        BudgetLedgerSummary summary = budgetRepository.findActiveSummary(new BudgetCheckCriteria(
                        command.departmentId(),
                        command.fiscalYear(),
                        command.glAccountCode()))
                .orElseThrow(() -> new BusinessException(ErrorCode.FIN_001));
        ensureCurrencyMatches(summary, command.requestAmount());

        BudgetCheckStatus status = summary.check(command.requestAmount());
        Money available = summary.available();
        String warningMessage = warningMessage(status, available, command.requestAmount());
        budgetAlertService.publishCheckResultIfNeeded(summary, command.requestAmount(), warningMessage);
        log.info("[ACTION] Complete CheckBudget | departmentId={} | budgetId={} | status={}",
                LogMaskingUtil.maskId(command.departmentId()),
                LogMaskingUtil.maskId(summary.id()),
                status);
        return new BudgetCheckView(
                summary.id(),
                summary.departmentId(),
                summary.fiscalYear(),
                summary.quarter(),
                summary.glAccountCode(),
                summary.allocated(),
                summary.committed(),
                summary.spent(),
                available,
                status,
                warningMessage);
    }

    private void ensureCurrencyMatches(BudgetLedgerSummary summary, Money requestAmount) {
        if (!summary.allocated().currency().equals(requestAmount.currency())) {
            throw new BusinessException(ErrorCode.FIN_003);
        }
    }

    private String warningMessage(BudgetCheckStatus status, Money available, Money requestAmount) {
        return switch (status) {
            case PASS -> null;
            case WARNING -> "Budget available after this request is below 20% threshold";
            case FAIL -> "Department budget is insufficient by "
                    + requestAmount.subtract(available).amount().toPlainString()
                    + " "
                    + requestAmount.currency();
        };
    }
}
