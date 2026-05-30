package com.eprocure.finance.application.usecase;

import com.eprocure.finance.application.port.in.ApproveBudgetOverrideCommand;
import com.eprocure.finance.application.port.out.BudgetDashboardCachePort;
import com.eprocure.finance.application.service.BudgetOverrideApprovalResult;
import com.eprocure.finance.application.service.BudgetOverrideApprovalView;
import com.eprocure.finance.application.service.IdempotencyService;
import com.eprocure.finance.common.exception.BusinessException;
import com.eprocure.finance.common.exception.ErrorCode;
import com.eprocure.finance.common.util.LogMaskingUtil;
import com.eprocure.finance.domain.model.BudgetLedgerSummary;
import com.eprocure.finance.domain.model.BudgetOverrideApproval;
import com.eprocure.finance.domain.model.BudgetOverrideStatus;
import com.eprocure.finance.domain.model.BudgetStatus;
import com.eprocure.finance.domain.model.vo.Money;
import com.eprocure.finance.domain.repository.BudgetRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApproveBudgetOverrideUseCase {
    private static final Logger log = LogManager.getLogger(ApproveBudgetOverrideUseCase.class);
    private static final String IDEMPOTENCY_OPERATION = "budget-override-approval";
    private static final int REASON_MIN_LENGTH = 50;

    private final BudgetRepository budgetRepository;
    private final BudgetDashboardCachePort cachePort;
    private final IdempotencyService idempotencyService;
    private final Clock clock;
    private final BigDecimal overrideThresholdPercent;

    public ApproveBudgetOverrideUseCase(
            BudgetRepository budgetRepository,
            BudgetDashboardCachePort cachePort,
            IdempotencyService idempotencyService,
            Clock clock,
            @Value("${eprocure.finance.budget-override-threshold-percent:30}") BigDecimal overrideThresholdPercent) {
        this.budgetRepository = budgetRepository;
        this.cachePort = cachePort;
        this.idempotencyService = idempotencyService;
        this.clock = clock;
        this.overrideThresholdPercent = overrideThresholdPercent;
    }

    @Transactional
    public BudgetOverrideApprovalResult execute(ApproveBudgetOverrideCommand command, String idempotencyKey) {
        Objects.requireNonNull(command, "command must not be null");
        idempotencyService.verify(idempotencyKey);
        var cached = idempotencyService.find(
                IDEMPOTENCY_OPERATION,
                command.actorId(),
                idempotencyKey,
                BudgetOverrideApprovalView.class);
        if (cached.isPresent()) {
            log.info("[ACTION] Idempotency hit ApproveBudgetOverride | budgetId={} | userId={} | key={}",
                    LogMaskingUtil.maskId(command.budgetId()),
                    LogMaskingUtil.maskId(command.actorId()),
                    LogMaskingUtil.maskToken(idempotencyKey));
            return BudgetOverrideApprovalResult.replayed(cached.get());
        }

        UUID key = UUID.fromString(idempotencyKey);
        var existing = budgetRepository.findOverrideApprovalByIdempotencyKey(key);
        if (existing.isPresent()) {
            BudgetOverrideApprovalView view = BudgetOverrideApprovalView.from(existing.get());
            idempotencyService.save(IDEMPOTENCY_OPERATION, command.actorId(), idempotencyKey, view);
            return BudgetOverrideApprovalResult.replayed(view);
        }

        validateCommand(command);
        log.info("[ACTION] Start ApproveBudgetOverride | budgetId={} | userId={}",
                LogMaskingUtil.maskId(command.budgetId()),
                LogMaskingUtil.maskId(command.actorId()));

        BudgetLedgerSummary budget = budgetRepository.findSummaryById(command.budgetId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FIN_001));
        ensureActive(budget);
        ensureCurrencyMatches(budget, command.overrideAmount());
        enforceThreshold(command.overrideAmount(), budget.allocated());

        BudgetOverrideApproval approval = new BudgetOverrideApproval(
                UUID.randomUUID(),
                budget.id(),
                command.purchaseRequestId(),
                command.overrideAmount(),
                command.overrideReason(),
                command.actorId(),
                Instant.now(clock),
                key,
                BudgetOverrideStatus.APPROVED);
        budgetRepository.insertOverrideApproval(approval);
        cachePort.evict(budget.id());

        BudgetOverrideApprovalView view = BudgetOverrideApprovalView.from(approval);
        idempotencyService.save(IDEMPOTENCY_OPERATION, command.actorId(), idempotencyKey, view);
        log.info("[ACTION] Complete ApproveBudgetOverride | budgetId={} | approvalId={}",
                LogMaskingUtil.maskId(budget.id()),
                LogMaskingUtil.maskId(approval.id()));
        return BudgetOverrideApprovalResult.fresh(view);
    }

    private void validateCommand(ApproveBudgetOverrideCommand command) {
        if (!command.overrideAmount().isPositive()) {
            throw new BusinessException(ErrorCode.FIN_002);
        }
        if (command.overrideReason() == null || command.overrideReason().length() < REASON_MIN_LENGTH) {
            throw new BusinessException(ErrorCode.VAL_001);
        }
    }

    private void ensureActive(BudgetLedgerSummary budget) {
        if (budget.status() != BudgetStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.FIN_005);
        }
    }

    private void ensureCurrencyMatches(BudgetLedgerSummary budget, Money amount) {
        if (!budget.allocated().currency().equals(amount.currency())) {
            throw new BusinessException(ErrorCode.FIN_003);
        }
    }

    private void enforceThreshold(Money overrideAmount, Money allocated) {
        BigDecimal threshold = allocated.amount()
                .multiply(overrideThresholdPercent)
                .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        if (overrideAmount.amount().compareTo(threshold) > 0) {
            throw new BusinessException(ErrorCode.FIN_004);
        }
    }
}
