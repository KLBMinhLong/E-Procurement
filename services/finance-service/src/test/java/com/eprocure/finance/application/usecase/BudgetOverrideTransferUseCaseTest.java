package com.eprocure.finance.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eprocure.finance.application.port.in.ApproveBudgetOverrideCommand;
import com.eprocure.finance.application.port.in.TransferBudgetCommand;
import com.eprocure.finance.application.port.out.BudgetAlertEventPublisher;
import com.eprocure.finance.application.port.out.BudgetDashboardCachePort;
import com.eprocure.finance.application.service.BudgetAlertService;
import com.eprocure.finance.application.service.BudgetDashboardView;
import com.eprocure.finance.application.service.IdempotencyService;
import com.eprocure.finance.common.exception.BusinessException;
import com.eprocure.finance.common.exception.ErrorCode;
import com.eprocure.finance.domain.event.BudgetAlertEvent;
import com.eprocure.finance.domain.model.BudgetAlertType;
import com.eprocure.finance.domain.model.BudgetCheckCriteria;
import com.eprocure.finance.domain.model.BudgetCommitmentHold;
import com.eprocure.finance.domain.model.BudgetLedgerSummary;
import com.eprocure.finance.domain.model.BudgetOverrideApproval;
import com.eprocure.finance.domain.model.BudgetOverrideStatus;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BudgetOverrideTransferUseCaseTest {
    private static final UUID ACTOR_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID SOURCE_BUDGET_ID = UUID.fromString("70000000-0000-4000-8000-000000000001");
    private static final UUID TARGET_BUDGET_ID = UUID.fromString("70000000-0000-4000-8000-000000000002");
    private static final UUID DEPARTMENT_ID = UUID.fromString("33333333-3333-4333-8333-333333333333");
    private static final UUID TARGET_DEPARTMENT_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID PR_ID = UUID.fromString("88000000-0000-4000-8000-000000000001");
    private static final String IDEMPOTENCY_KEY = "20000000-0000-4000-8000-000000000001";
    private static final Instant NOW = Instant.parse("2026-05-30T03:00:00Z");
    private static final String LONG_REASON =
            "Finance approved this exceptional budget movement for urgent production continuity.";

    private FakeBudgetRepository budgetRepository;
    private FakeBudgetDashboardCachePort cachePort;
    private FakeIdempotencyService idempotencyService;
    private FakeBudgetAlertEventPublisher alertPublisher;
    private BudgetAlertService budgetAlertService;
    private Clock clock;

    @BeforeEach
    void setUp() {
        budgetRepository = new FakeBudgetRepository();
        cachePort = new FakeBudgetDashboardCachePort();
        idempotencyService = new FakeIdempotencyService();
        alertPublisher = new FakeBudgetAlertEventPublisher();
        clock = Clock.fixed(NOW, ZoneOffset.UTC);
        budgetAlertService = new BudgetAlertService(alertPublisher, clock);
    }

    @Test
    void should_record_override_approval_without_mutating_allocated_budget() {
        budgetRepository.put(summary(SOURCE_BUDGET_ID, DEPARTMENT_ID, "500000000.0000", "100000000.0000", "50000000.0000"));
        var useCase = new ApproveBudgetOverrideUseCase(
                budgetRepository, cachePort, idempotencyService, clock, new BigDecimal("30"));

        var result = useCase.execute(new ApproveBudgetOverrideCommand(
                ACTOR_ID,
                SOURCE_BUDGET_ID,
                PR_ID,
                money("100000000.0000"),
                LONG_REASON), IDEMPOTENCY_KEY);

        assertThat(result.replayed()).isFalse();
        assertThat(budgetRepository.overrideApprovals).hasSize(1);
        assertThat(budgetRepository.overrideApprovals.get(0).status()).isEqualTo(BudgetOverrideStatus.APPROVED);
        assertThat(budgetRepository.adjustments).isEmpty();
        assertThat(cachePort.evictedBudgetIds).contains(SOURCE_BUDGET_ID);
        assertThat(result.view().overrideAmount().amount()).isEqualByComparingTo("100000000.0000");
    }

    @Test
    void should_throw_fin_004_when_override_exceeds_configured_threshold() {
        budgetRepository.put(summary(SOURCE_BUDGET_ID, DEPARTMENT_ID, "500000000.0000", "0.0000", "0.0000"));
        var useCase = new ApproveBudgetOverrideUseCase(
                budgetRepository, cachePort, idempotencyService, clock, new BigDecimal("30"));

        assertThatThrownBy(() -> useCase.execute(new ApproveBudgetOverrideCommand(
                ACTOR_ID,
                SOURCE_BUDGET_ID,
                PR_ID,
                money("200000000.0000"),
                LONG_REASON), IDEMPOTENCY_KEY))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FIN_004);
    }

    @Test
    void should_transfer_budget_and_write_balanced_ledger_transactions() {
        budgetRepository.put(summary(SOURCE_BUDGET_ID, DEPARTMENT_ID, "500000000.0000", "100000000.0000", "50000000.0000"));
        budgetRepository.put(summary(TARGET_BUDGET_ID, TARGET_DEPARTMENT_ID, "200000000.0000", "0.0000", "0.0000"));
        var useCase = new TransferBudgetUseCase(
                budgetRepository, cachePort, idempotencyService, budgetAlertService, clock);

        var result = useCase.execute(new TransferBudgetCommand(
                ACTOR_ID,
                SOURCE_BUDGET_ID,
                TARGET_BUDGET_ID,
                money("50000000.0000"),
                "Move approved budget to balance urgent team spend."), IDEMPOTENCY_KEY);

        assertThat(result.replayed()).isFalse();
        assertThat(budgetRepository.transfers).hasSize(1);
        assertThat(budgetRepository.transactions).extracting(BudgetTransaction::transactionType)
                .containsExactly(BudgetTransactionType.TRANSFER_OUT, BudgetTransactionType.TRANSFER_IN);
        assertThat(result.view().sourceDashboard().allocated().amount()).isEqualByComparingTo("450000000.0000");
        assertThat(result.view().targetDashboard().allocated().amount()).isEqualByComparingTo("250000000.0000");
        assertThat(cachePort.evictedBudgetIds).contains(SOURCE_BUDGET_ID, TARGET_BUDGET_ID);
        assertThat(alertPublisher.events).isEmpty();
    }

    @Test
    void should_publish_warning_when_transfer_source_drops_below_threshold() {
        budgetRepository.put(summary(SOURCE_BUDGET_ID, DEPARTMENT_ID, "100000000.0000", "60000000.0000", "0.0000"));
        budgetRepository.put(summary(TARGET_BUDGET_ID, TARGET_DEPARTMENT_ID, "200000000.0000", "0.0000", "0.0000"));
        var useCase = new TransferBudgetUseCase(
                budgetRepository, cachePort, idempotencyService, budgetAlertService, clock);

        useCase.execute(new TransferBudgetCommand(
                ACTOR_ID,
                SOURCE_BUDGET_ID,
                TARGET_BUDGET_ID,
                money("26000000.0000"),
                "Move approved budget to balance urgent team spend."), IDEMPOTENCY_KEY);

        assertThat(alertPublisher.events).hasSize(1);
        assertThat(alertPublisher.events.get(0).payload().alertType()).isEqualTo(BudgetAlertType.WARNING);
        assertThat(alertPublisher.events.get(0).payload().referenceType()).isEqualTo("BUDGET_TRANSFER");
        assertThat(alertPublisher.events.get(0).payload().projectedAvailable().amount())
                .isEqualByComparingTo("14000000.0000");
    }

    @Test
    void should_throw_fin_009_when_source_budget_available_is_insufficient() {
        budgetRepository.put(summary(SOURCE_BUDGET_ID, DEPARTMENT_ID, "500000000.0000", "450000000.0000", "40000000.0000"));
        budgetRepository.put(summary(TARGET_BUDGET_ID, TARGET_DEPARTMENT_ID, "200000000.0000", "0.0000", "0.0000"));
        var useCase = new TransferBudgetUseCase(
                budgetRepository, cachePort, idempotencyService, budgetAlertService, clock);

        assertThatThrownBy(() -> useCase.execute(new TransferBudgetCommand(
                ACTOR_ID,
                SOURCE_BUDGET_ID,
                TARGET_BUDGET_ID,
                money("20000000.0000"),
                "Move approved budget to balance urgent team spend."), IDEMPOTENCY_KEY))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FIN_009);
    }

    @Test
    void should_replay_existing_transfer_by_idempotency_key_without_duplicate_mutation() {
        budgetRepository.put(summary(SOURCE_BUDGET_ID, DEPARTMENT_ID, "450000000.0000", "100000000.0000", "50000000.0000"));
        budgetRepository.put(summary(TARGET_BUDGET_ID, TARGET_DEPARTMENT_ID, "250000000.0000", "0.0000", "0.0000"));
        BudgetTransfer existing = new BudgetTransfer(
                UUID.fromString("90000000-0000-4000-8000-000000000001"),
                SOURCE_BUDGET_ID,
                TARGET_BUDGET_ID,
                money("50000000.0000"),
                "Move approved budget to balance urgent team spend.",
                ACTOR_ID,
                NOW,
                UUID.fromString(IDEMPOTENCY_KEY));
        budgetRepository.existingTransfer = existing;
        var useCase = new TransferBudgetUseCase(
                budgetRepository, cachePort, idempotencyService, budgetAlertService, clock);

        var result = useCase.execute(new TransferBudgetCommand(
                ACTOR_ID,
                SOURCE_BUDGET_ID,
                TARGET_BUDGET_ID,
                money("50000000.0000"),
                "Move approved budget to balance urgent team spend."), IDEMPOTENCY_KEY);

        assertThat(result.replayed()).isTrue();
        assertThat(budgetRepository.transfers).isEmpty();
        assertThat(budgetRepository.transactions).isEmpty();
        assertThat(budgetRepository.adjustments).isEmpty();
        assertThat(alertPublisher.events).isEmpty();
    }

    private static BudgetLedgerSummary summary(
            UUID budgetId,
            UUID departmentId,
            String allocated,
            String committed,
            String spent) {
        return new BudgetLedgerSummary(
                budgetId,
                departmentId,
                2026,
                null,
                "6002",
                money(allocated),
                money(committed),
                money(spent),
                BudgetStatus.ACTIVE);
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

    private static final class FakeIdempotencyService extends IdempotencyService {
        private final Map<String, Object> cache = new HashMap<>();

        private FakeIdempotencyService() {
            super(null, null);
        }

        @Override
        public void verify(String idempotencyKey) {
            if (idempotencyKey == null || idempotencyKey.isBlank()) {
                throw new BusinessException(ErrorCode.SYS_005);
            }
        }

        @Override
        public <T> Optional<T> find(String operation, UUID actorId, String idempotencyKey, Class<T> type) {
            Object value = cache.get(operation + actorId + idempotencyKey);
            return type.isInstance(value) ? Optional.of(type.cast(value)) : Optional.empty();
        }

        @Override
        public void save(String operation, UUID actorId, String idempotencyKey, Object response) {
            cache.put(operation + actorId + idempotencyKey, response);
        }
    }

    private static final class FakeBudgetAlertEventPublisher implements BudgetAlertEventPublisher {
        private final List<BudgetAlertEvent> events = new ArrayList<>();

        @Override
        public void publish(BudgetAlertEvent event) {
            events.add(event);
        }
    }

    private static final class FakeBudgetRepository implements BudgetRepository {
        private final Map<UUID, BudgetLedgerSummary> summaries = new HashMap<>();
        private final List<BudgetOverrideApproval> overrideApprovals = new ArrayList<>();
        private final List<BudgetTransfer> transfers = new ArrayList<>();
        private final List<BudgetTransaction> transactions = new ArrayList<>();
        private final List<UUID> lockedBudgetIds = new ArrayList<>();
        private final List<Money> adjustments = new ArrayList<>();
        private BudgetTransfer existingTransfer;

        private void put(BudgetLedgerSummary summary) {
            summaries.put(summary.id(), summary);
        }

        @Override
        public Optional<BudgetLedgerSummary> findActiveSummary(BudgetCheckCriteria criteria) {
            return Optional.empty();
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
            return Optional.ofNullable(summaries.get(budgetId));
        }

        @Override
        public boolean lockBudgetForUpdate(UUID budgetId) {
            lockedBudgetIds.add(budgetId);
            return summaries.containsKey(budgetId);
        }

        @Override
        public boolean existsProcessedEvent(String eventId) {
            return false;
        }

        @Override
        public void markEventProcessed(String eventId, String topic, Integer partitionId, Long offsetValue, String handlerName) {
        }

        @Override
        public boolean existsTransaction(UUID budgetId, BudgetTransactionType transactionType, String referenceType, UUID referenceId) {
            return false;
        }

        @Override
        public void insertTransaction(BudgetTransaction transaction) {
            transactions.add(transaction);
        }

        @Override
        public Optional<BudgetCommitmentHold> findHeldCommitment(String referenceType, UUID referenceId) {
            return Optional.empty();
        }

        @Override
        public Optional<BudgetOverrideApproval> findOverrideApprovalByIdempotencyKey(UUID idempotencyKey) {
            return overrideApprovals.stream()
                    .filter(approval -> approval.idempotencyKey().equals(idempotencyKey))
                    .findFirst();
        }

        @Override
        public void insertOverrideApproval(BudgetOverrideApproval approval) {
            overrideApprovals.add(approval);
        }

        @Override
        public Optional<BudgetTransfer> findTransferByIdempotencyKey(UUID idempotencyKey) {
            if (existingTransfer != null && existingTransfer.idempotencyKey().equals(idempotencyKey)) {
                return Optional.of(existingTransfer);
            }
            return transfers.stream()
                    .filter(transfer -> transfer.idempotencyKey().equals(idempotencyKey))
                    .findFirst();
        }

        @Override
        public void insertTransfer(BudgetTransfer transfer) {
            transfers.add(transfer);
        }

        @Override
        public void adjustAllocatedAmount(UUID budgetId, Money delta, UUID actorId) {
            adjustments.add(delta);
            BudgetLedgerSummary current = summaries.get(budgetId);
            summaries.put(budgetId, new BudgetLedgerSummary(
                    current.id(),
                    current.departmentId(),
                    current.fiscalYear(),
                    current.quarter(),
                    current.glAccountCode(),
                    current.allocated().add(delta),
                    current.committed(),
                    current.spent(),
                    current.status()));
        }
    }
}
