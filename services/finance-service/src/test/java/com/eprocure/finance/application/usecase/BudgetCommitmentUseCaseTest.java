package com.eprocure.finance.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.eprocure.finance.application.port.in.RecordBudgetCommitmentCommand;
import com.eprocure.finance.application.port.in.ReleaseBudgetCommitmentCommand;
import com.eprocure.finance.application.port.out.BudgetAlertEventPublisher;
import com.eprocure.finance.application.port.out.BudgetDashboardCachePort;
import com.eprocure.finance.application.service.BudgetAlertService;
import com.eprocure.finance.application.service.BudgetDashboardView;
import com.eprocure.finance.domain.event.BudgetAlertEvent;
import com.eprocure.finance.domain.model.BudgetAlertType;
import com.eprocure.finance.domain.model.BudgetCheckCriteria;
import com.eprocure.finance.domain.model.BudgetCommitmentHold;
import com.eprocure.finance.domain.model.BudgetLedgerSummary;
import com.eprocure.finance.domain.model.BudgetOverrideApproval;
import com.eprocure.finance.domain.model.BudgetStatus;
import com.eprocure.finance.domain.model.BudgetTransaction;
import com.eprocure.finance.domain.model.BudgetTransactionType;
import com.eprocure.finance.domain.model.BudgetTransfer;
import com.eprocure.finance.domain.model.vo.Money;
import com.eprocure.finance.domain.repository.BudgetFilter;
import com.eprocure.finance.domain.repository.BudgetRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BudgetCommitmentUseCaseTest {
    private static final UUID BUDGET_ID = UUID.fromString("70000000-0000-4000-8000-000000000001");
    private static final UUID DEPARTMENT_ID = UUID.fromString("33333333-3333-4333-8333-333333333333");
    private static final UUID PR_ID = UUID.fromString("88000000-0000-4000-8000-000000000001");
    private static final Instant OCCURRED_AT = Instant.parse("2026-05-30T02:00:00Z");

    private FakeBudgetRepository budgetRepository;
    private FakeBudgetDashboardCachePort cachePort;
    private FakeBudgetAlertEventPublisher alertPublisher;
    private BudgetAlertService budgetAlertService;

    @BeforeEach
    void setUp() {
        budgetRepository = new FakeBudgetRepository();
        cachePort = new FakeBudgetDashboardCachePort();
        alertPublisher = new FakeBudgetAlertEventPublisher();
        budgetAlertService = new BudgetAlertService(alertPublisher, Clock.fixed(OCCURRED_AT, ZoneOffset.UTC));
    }

    @Test
    void should_create_tentative_commit_when_pr_submitted_event_arrives() {
        var useCase = new TentativeCommitBudgetUseCase(budgetRepository, cachePort, budgetAlertService);

        useCase.execute(recordCommand("evt-submitted-001", "procurement.pr.submitted"));

        assertThat(budgetRepository.transactions).hasSize(1);
        assertThat(budgetRepository.transactions.get(0).transactionType()).isEqualTo(BudgetTransactionType.COMMIT_TENTATIVE);
        assertThat(budgetRepository.processedEvents).contains("evt-submitted-001");
        assertThat(cachePort.evictedBudgetIds).contains(BUDGET_ID);
        assertThat(alertPublisher.events).isEmpty();
    }

    @Test
    void should_publish_warning_when_tentative_commit_drops_budget_below_threshold() {
        budgetRepository.summary = summary("100000000.0000", "15000000.0000", "0.0000");
        var useCase = new TentativeCommitBudgetUseCase(budgetRepository, cachePort, budgetAlertService);

        useCase.execute(recordCommand("evt-submitted-low-001", "procurement.pr.submitted"));

        assertThat(alertPublisher.events).hasSize(1);
        assertThat(alertPublisher.events.get(0).payload().alertType()).isEqualTo(BudgetAlertType.WARNING);
        assertThat(alertPublisher.events.get(0).payload().sourceEventId()).isEqualTo("evt-submitted-low-001");
        assertThat(alertPublisher.events.get(0).payload().projectedAvailablePercent()).isEqualByComparingTo("15.00");
    }

    @Test
    void should_skip_tentative_commit_when_event_already_processed() {
        budgetRepository.processedEvents.add("evt-submitted-001");
        var useCase = new TentativeCommitBudgetUseCase(budgetRepository, cachePort, budgetAlertService);

        useCase.execute(recordCommand("evt-submitted-001", "procurement.pr.submitted"));

        assertThat(budgetRepository.transactions).isEmpty();
        assertThat(cachePort.evictedBudgetIds).isEmpty();
        assertThat(alertPublisher.events).isEmpty();
    }

    @Test
    void should_release_tentative_and_create_firm_commit_when_pr_approved() {
        budgetRepository.hold = new BudgetCommitmentHold(BUDGET_ID, money("70000000.0000"));
        var useCase = new FirmCommitBudgetUseCase(budgetRepository, cachePort, budgetAlertService);

        useCase.execute(recordCommand("evt-approved-001", "procurement.pr.approved"));

        assertThat(budgetRepository.transactions).extracting(BudgetTransaction::transactionType)
                .containsExactly(BudgetTransactionType.RELEASE, BudgetTransactionType.COMMIT_FIRM);
        assertThat(budgetRepository.processedEvents).contains("evt-approved-001");
        assertThat(cachePort.evictedBudgetIds).contains(BUDGET_ID);
    }

    @Test
    void should_release_held_commitment_when_pr_rejected() {
        budgetRepository.hold = new BudgetCommitmentHold(BUDGET_ID, money("70000000.0000"));
        var useCase = new ReleaseBudgetCommitmentUseCase(budgetRepository, cachePort);

        useCase.execute(releaseCommand("evt-rejected-001", "procurement.pr.rejected"));

        assertThat(budgetRepository.transactions).hasSize(1);
        assertThat(budgetRepository.transactions.get(0).transactionType()).isEqualTo(BudgetTransactionType.RELEASE);
        assertThat(budgetRepository.processedEvents).contains("evt-rejected-001");
        assertThat(cachePort.evictedBudgetIds).contains(BUDGET_ID);
    }

    private static RecordBudgetCommitmentCommand recordCommand(String eventId, String topic) {
        return new RecordBudgetCommitmentCommand(
                eventId,
                topic,
                0,
                10L,
                PR_ID,
                "PR-2026-05-00001",
                DEPARTMENT_ID,
                2026,
                "6002",
                money("70000000.0000"),
                OCCURRED_AT);
    }

    private static ReleaseBudgetCommitmentCommand releaseCommand(String eventId, String topic) {
        return new ReleaseBudgetCommitmentCommand(
                eventId,
                topic,
                0,
                10L,
                PR_ID,
                "PR-2026-05-00001",
                OCCURRED_AT);
    }

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount), "VND");
    }

    private static final class FakeBudgetDashboardCachePort implements BudgetDashboardCachePort {
        private final List<UUID> evictedBudgetIds = new ArrayList<>();

        @Override
        public Optional<BudgetDashboardView> findByBudgetId(UUID budgetId) {
            return Optional.empty();
        }

        @Override
        public void store(BudgetDashboardView dashboard) {
        }

        @Override
        public void evict(UUID budgetId) {
            evictedBudgetIds.add(budgetId);
        }
    }

    private static BudgetLedgerSummary summary() {
        return summary("500000000.0000", "0.0000", "0.0000");
    }

    private static BudgetLedgerSummary summary(String allocated, String committed, String spent) {
        return new BudgetLedgerSummary(
                BUDGET_ID,
                DEPARTMENT_ID,
                2026,
                null,
                "6002",
                money(allocated),
                money(committed),
                money(spent),
                BudgetStatus.ACTIVE);
    }

    private static final class FakeBudgetRepository implements BudgetRepository {
        private final Set<String> processedEvents = new HashSet<>();
        private final List<BudgetTransaction> transactions = new ArrayList<>();
        private BudgetLedgerSummary summary = summary();
        private BudgetCommitmentHold hold;

        @Override
        public Optional<BudgetLedgerSummary> findActiveSummary(BudgetCheckCriteria criteria) {
            return Optional.of(summary);
        }

        @Override
        public List<BudgetLedgerSummary> findByFilter(BudgetFilter filter) {
            return List.of();
        }

        @Override
        public long countByFilter(BudgetFilter filter) {
            return 0;
        }

        @Override
        public Optional<BudgetLedgerSummary> findSummaryById(UUID budgetId) {
            if (!BUDGET_ID.equals(budgetId)) {
                return Optional.empty();
            }
            Money committed = transactions.stream()
                    .filter(transaction -> transaction.budgetId().equals(budgetId))
                    .map(transaction -> transaction.transactionType() == BudgetTransactionType.RELEASE
                            ? new Money(transaction.money().amount().negate(), transaction.money().currency())
                            : transaction.money())
                    .reduce(summary.committed(), Money::add);
            return Optional.of(new BudgetLedgerSummary(
                    summary.id(),
                    summary.departmentId(),
                    summary.fiscalYear(),
                    summary.quarter(),
                    summary.glAccountCode(),
                    summary.allocated(),
                    committed,
                    summary.spent(),
                    summary.status()));
        }

        @Override
        public boolean lockBudgetForUpdate(UUID budgetId) {
            return true;
        }

        @Override
        public boolean existsProcessedEvent(String eventId) {
            return processedEvents.contains(eventId);
        }

        @Override
        public void markEventProcessed(String eventId, String topic, Integer partitionId, Long offsetValue, String handlerName) {
            processedEvents.add(eventId);
        }

        @Override
        public boolean existsTransaction(UUID budgetId, BudgetTransactionType transactionType, String referenceType, UUID referenceId) {
            return transactions.stream()
                    .anyMatch(transaction -> transaction.budgetId().equals(budgetId)
                            && transaction.transactionType() == transactionType
                            && transaction.referenceType().equals(referenceType)
                            && transaction.referenceId().equals(referenceId));
        }

        @Override
        public void insertTransaction(BudgetTransaction transaction) {
            transactions.add(transaction);
        }

        @Override
        public Optional<BudgetCommitmentHold> findHeldCommitment(String referenceType, UUID referenceId) {
            return Optional.ofNullable(hold);
        }

        @Override
        public Optional<BudgetOverrideApproval> findOverrideApprovalByIdempotencyKey(UUID idempotencyKey) {
            return Optional.empty();
        }

        @Override
        public void insertOverrideApproval(BudgetOverrideApproval approval) {
        }

        @Override
        public Optional<BudgetTransfer> findTransferByIdempotencyKey(UUID idempotencyKey) {
            return Optional.empty();
        }

        @Override
        public void insertTransfer(BudgetTransfer transfer) {
        }

        @Override
        public void adjustAllocatedAmount(UUID budgetId, Money delta, UUID actorId) {
        }
    }

    private static final class FakeBudgetAlertEventPublisher implements BudgetAlertEventPublisher {
        private final List<BudgetAlertEvent> events = new ArrayList<>();

        @Override
        public void publish(BudgetAlertEvent event) {
            events.add(event);
        }
    }
}
