package com.eprocure.finance.application.usecase;

import com.eprocure.finance.application.port.in.TransferBudgetCommand;
import com.eprocure.finance.application.port.out.BudgetDashboardCachePort;
import com.eprocure.finance.application.service.BudgetAlertService;
import com.eprocure.finance.application.service.BudgetDashboardView;
import com.eprocure.finance.application.service.BudgetTransferResult;
import com.eprocure.finance.application.service.BudgetTransferView;
import com.eprocure.finance.application.service.IdempotencyService;
import com.eprocure.finance.common.exception.BusinessException;
import com.eprocure.finance.common.exception.ErrorCode;
import com.eprocure.finance.common.util.LogMaskingUtil;
import com.eprocure.finance.domain.model.BudgetLedgerSummary;
import com.eprocure.finance.domain.model.BudgetStatus;
import com.eprocure.finance.domain.model.BudgetTransaction;
import com.eprocure.finance.domain.model.BudgetTransactionType;
import com.eprocure.finance.domain.model.BudgetTransfer;
import com.eprocure.finance.domain.model.vo.Money;
import com.eprocure.finance.domain.repository.BudgetRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferBudgetUseCase {
    private static final Logger log = LogManager.getLogger(TransferBudgetUseCase.class);
    private static final String IDEMPOTENCY_OPERATION = "budget-transfer";
    private static final String REFERENCE_TYPE = "BUDGET_TRANSFER";
    private static final int REASON_MIN_LENGTH = 20;

    private final BudgetRepository budgetRepository;
    private final BudgetDashboardCachePort cachePort;
    private final IdempotencyService idempotencyService;
    private final BudgetAlertService budgetAlertService;
    private final Clock clock;

    public TransferBudgetUseCase(
            BudgetRepository budgetRepository,
            BudgetDashboardCachePort cachePort,
            IdempotencyService idempotencyService,
            BudgetAlertService budgetAlertService,
            Clock clock) {
        this.budgetRepository = budgetRepository;
        this.cachePort = cachePort;
        this.idempotencyService = idempotencyService;
        this.budgetAlertService = budgetAlertService;
        this.clock = clock;
    }

    @Transactional
    public BudgetTransferResult execute(TransferBudgetCommand command, String idempotencyKey) {
        Objects.requireNonNull(command, "command must not be null");
        idempotencyService.verify(idempotencyKey);
        var cached = idempotencyService.find(
                IDEMPOTENCY_OPERATION,
                command.actorId(),
                idempotencyKey,
                BudgetTransferView.class);
        if (cached.isPresent()) {
            log.info("[ACTION] Idempotency hit TransferBudget | sourceBudgetId={} | userId={} | key={}",
                    LogMaskingUtil.maskId(command.sourceBudgetId()),
                    LogMaskingUtil.maskId(command.actorId()),
                    LogMaskingUtil.maskToken(idempotencyKey));
            return BudgetTransferResult.replayed(cached.get());
        }

        UUID key = UUID.fromString(idempotencyKey);
        var existing = budgetRepository.findTransferByIdempotencyKey(key);
        if (existing.isPresent()) {
            BudgetTransferView view = toView(existing.get());
            idempotencyService.save(IDEMPOTENCY_OPERATION, command.actorId(), idempotencyKey, view);
            return BudgetTransferResult.replayed(view);
        }

        validateCommand(command);
        log.info("[ACTION] Start TransferBudget | sourceBudgetId={} | targetBudgetId={} | userId={}",
                LogMaskingUtil.maskId(command.sourceBudgetId()),
                LogMaskingUtil.maskId(command.targetBudgetId()),
                LogMaskingUtil.maskId(command.actorId()));

        lockBudgets(command.sourceBudgetId(), command.targetBudgetId());
        BudgetLedgerSummary source = findBudget(command.sourceBudgetId());
        BudgetLedgerSummary target = findBudget(command.targetBudgetId());
        validateTransfer(command, source, target);

        Instant now = Instant.now(clock);
        BudgetTransfer transfer = new BudgetTransfer(
                UUID.randomUUID(),
                source.id(),
                target.id(),
                command.amount(),
                command.reason(),
                command.actorId(),
                now,
                key);

        budgetRepository.insertTransfer(transfer);
        budgetRepository.adjustAllocatedAmount(source.id(), negate(command.amount()), command.actorId());
        budgetRepository.adjustAllocatedAmount(target.id(), command.amount(), command.actorId());
        insertTransferLedger(transfer, command.actorId(), now, idempotencyKey);

        cachePort.evict(source.id());
        cachePort.evict(target.id());
        publishSourceAlert(transfer);
        BudgetTransferView view = toView(transfer);
        idempotencyService.save(IDEMPOTENCY_OPERATION, command.actorId(), idempotencyKey, view);
        log.info("[ACTION] Complete TransferBudget | transferId={} | sourceBudgetId={} | targetBudgetId={}",
                LogMaskingUtil.maskId(transfer.id()),
                LogMaskingUtil.maskId(source.id()),
                LogMaskingUtil.maskId(target.id()));
        return BudgetTransferResult.fresh(view);
    }

    private void validateCommand(TransferBudgetCommand command) {
        if (command.sourceBudgetId().equals(command.targetBudgetId())) {
            throw new BusinessException(ErrorCode.VAL_001);
        }
        if (!command.amount().isPositive()) {
            throw new BusinessException(ErrorCode.FIN_002);
        }
        if (command.reason() == null || command.reason().length() < REASON_MIN_LENGTH) {
            throw new BusinessException(ErrorCode.VAL_001);
        }
    }

    private void lockBudgets(UUID sourceBudgetId, UUID targetBudgetId) {
        List.of(sourceBudgetId, targetBudgetId).stream()
                .sorted(Comparator.comparing(UUID::toString))
                .forEach(budgetRepository::lockBudgetForUpdate);
    }

    private BudgetLedgerSummary findBudget(UUID budgetId) {
        return budgetRepository.findSummaryById(budgetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FIN_001));
    }

    private void validateTransfer(TransferBudgetCommand command, BudgetLedgerSummary source, BudgetLedgerSummary target) {
        if (source.status() != BudgetStatus.ACTIVE || target.status() != BudgetStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.FIN_005);
        }
        if (source.fiscalYear() != target.fiscalYear()) {
            throw new BusinessException(ErrorCode.VAL_001);
        }
        if (!source.allocated().currency().equals(target.allocated().currency())
                || !source.allocated().currency().equals(command.amount().currency())) {
            throw new BusinessException(ErrorCode.FIN_003);
        }
        if (source.available().compareTo(command.amount()) < 0) {
            throw new BusinessException(ErrorCode.FIN_009);
        }
    }

    private void insertTransferLedger(BudgetTransfer transfer, UUID actorId, Instant performedAt, String idempotencyKey) {
        budgetRepository.insertTransaction(new BudgetTransaction(
                transfer.sourceBudgetId(),
                BudgetTransactionType.TRANSFER_OUT,
                transfer.money(),
                REFERENCE_TYPE,
                transfer.id(),
                "Budget transfer out",
                actorId,
                performedAt,
                idempotencyKey));
        budgetRepository.insertTransaction(new BudgetTransaction(
                transfer.targetBudgetId(),
                BudgetTransactionType.TRANSFER_IN,
                transfer.money(),
                REFERENCE_TYPE,
                transfer.id(),
                "Budget transfer in",
                actorId,
                performedAt,
                idempotencyKey));
    }

    private BudgetTransferView toView(BudgetTransfer transfer) {
        BudgetDashboardView sourceDashboard = BudgetDashboardView.from(findBudget(transfer.sourceBudgetId()));
        BudgetDashboardView targetDashboard = BudgetDashboardView.from(findBudget(transfer.targetBudgetId()));
        return BudgetTransferView.from(transfer, sourceDashboard, targetDashboard);
    }

    private void publishSourceAlert(BudgetTransfer transfer) {
        budgetRepository.findSummaryById(transfer.sourceBudgetId())
                .ifPresent(summary -> budgetAlertService.publishIfNeeded(
                        summary,
                        transfer.money(),
                        REFERENCE_TYPE,
                        transfer.id(),
                        transfer.idempotencyKey().toString(),
                        "Source budget is below policy threshold after transfer"));
    }

    private Money negate(Money money) {
        return new Money(money.amount().negate(), money.currency());
    }
}
