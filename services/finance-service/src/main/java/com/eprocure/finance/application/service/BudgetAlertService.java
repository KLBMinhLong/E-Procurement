package com.eprocure.finance.application.service;

import com.eprocure.finance.application.port.out.BudgetAlertEventPublisher;
import com.eprocure.finance.domain.event.BudgetAlertEvent;
import com.eprocure.finance.domain.model.BudgetAlertType;
import com.eprocure.finance.domain.model.BudgetLedgerSummary;
import com.eprocure.finance.domain.model.vo.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class BudgetAlertService {
    private static final BigDecimal WARNING_AVAILABLE_RATIO = new BigDecimal("0.20");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final BudgetAlertEventPublisher eventPublisher;
    private final Clock clock;

    public BudgetAlertService(BudgetAlertEventPublisher eventPublisher, Clock clock) {
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    public void publishIfNeeded(
            BudgetLedgerSummary summary,
            Money impactAmount,
            String referenceType,
            UUID referenceId,
            String sourceEventId,
            String reason) {
        Objects.requireNonNull(summary, "summary must not be null");
        Money projectedAvailable = summary.available();
        publishIfNeeded(summary, projectedAvailable, impactAmount, referenceType, referenceId, sourceEventId, reason);
    }

    public void publishCheckResultIfNeeded(
            BudgetLedgerSummary summary,
            Money requestAmount,
            String reason) {
        Objects.requireNonNull(summary, "summary must not be null");
        Objects.requireNonNull(requestAmount, "requestAmount must not be null");
        Money projectedAvailable = summary.available().subtract(requestAmount);
        publishIfNeeded(summary, projectedAvailable, requestAmount, "BUDGET_CHECK", summary.id(), null, reason);
    }

    private void publishIfNeeded(
            BudgetLedgerSummary summary,
            Money projectedAvailable,
            Money impactAmount,
            String referenceType,
            UUID referenceId,
            String sourceEventId,
            String reason) {
        alertType(summary, projectedAvailable)
                .map(type -> toEvent(type, summary, projectedAvailable, impactAmount,
                        referenceType, referenceId, sourceEventId, reason))
                .ifPresent(eventPublisher::publish);
    }

    private Optional<BudgetAlertType> alertType(BudgetLedgerSummary summary, Money projectedAvailable) {
        if (projectedAvailable.isNegative()) {
            return Optional.of(BudgetAlertType.EXCEEDED);
        }
        if (summary.allocated().amount().signum() == 0) {
            return Optional.of(BudgetAlertType.EXCEEDED);
        }
        BigDecimal ratio = projectedAvailable.amount()
                .divide(summary.allocated().amount(), 6, RoundingMode.HALF_UP);
        return ratio.compareTo(WARNING_AVAILABLE_RATIO) < 0
                ? Optional.of(BudgetAlertType.WARNING)
                : Optional.empty();
    }

    private BudgetAlertEvent toEvent(
            BudgetAlertType alertType,
            BudgetLedgerSummary summary,
            Money projectedAvailable,
            Money impactAmount,
            String referenceType,
            UUID referenceId,
            String sourceEventId,
            String reason) {
        Instant now = Instant.now(clock);
        return BudgetAlertEvent.create(alertType, now, new BudgetAlertEvent.Payload(
                alertType,
                summary.id(),
                summary.departmentId(),
                summary.fiscalYear(),
                summary.quarter(),
                summary.glAccountCode(),
                summary.allocated(),
                summary.committed(),
                summary.spent(),
                summary.available(),
                projectedAvailable,
                availablePercent(summary, projectedAvailable),
                impactAmount,
                referenceType,
                referenceId,
                sourceEventId,
                reason));
    }

    private BigDecimal availablePercent(BudgetLedgerSummary summary, Money projectedAvailable) {
        if (summary.allocated().amount().signum() == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return projectedAvailable.amount()
                .multiply(ONE_HUNDRED)
                .divide(summary.allocated().amount(), 2, RoundingMode.HALF_UP);
    }
}
