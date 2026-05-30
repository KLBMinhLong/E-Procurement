package com.eprocure.finance.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eprocure.finance.application.port.in.GetBudgetDashboardQuery;
import com.eprocure.finance.application.port.in.ListBudgetsQuery;
import com.eprocure.finance.application.port.out.BudgetDashboardCachePort;
import com.eprocure.finance.application.service.BudgetDashboardView;
import com.eprocure.finance.common.exception.BusinessException;
import com.eprocure.finance.common.exception.ErrorCode;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BudgetDashboardUseCaseTest {
    private static final UUID ACTOR_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID OWN_DEPARTMENT_ID = UUID.fromString("33333333-3333-4333-8333-333333333333");
    private static final UUID OTHER_DEPARTMENT_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID BUDGET_ID = UUID.fromString("70000000-0000-4000-8000-000000000001");

    private FakeBudgetRepository budgetRepository;
    private FakeBudgetDashboardCachePort cachePort;

    @BeforeEach
    void setUp() {
        budgetRepository = new FakeBudgetRepository();
        cachePort = new FakeBudgetDashboardCachePort();
    }

    @Test
    void should_list_own_department_budgets_when_user_has_own_department_permission() {
        budgetRepository.summaries.add(summary(BUDGET_ID, OWN_DEPARTMENT_ID));
        var useCase = new ListBudgetsUseCase(budgetRepository);

        var result = useCase.execute(listQuery(Set.of("BUDGET_VIEW_OWN_DEPT"), null));

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).available().amount()).isEqualByComparingTo("350000000.0000");
        assertThat(result.meta().totalElements()).isEqualTo(1);
        assertThat(budgetRepository.lastFilter.departmentId()).isEqualTo(OWN_DEPARTMENT_ID);
    }

    @Test
    void should_return_empty_page_when_own_department_user_filters_other_department() {
        var useCase = new ListBudgetsUseCase(budgetRepository);

        var result = useCase.execute(listQuery(Set.of("BUDGET_VIEW_OWN_DEPT"), OTHER_DEPARTMENT_ID));

        assertThat(result.items()).isEmpty();
        assertThat(result.meta().totalElements()).isZero();
        assertThat(budgetRepository.lastFilter).isNull();
    }

    @Test
    void should_list_all_budgets_when_user_has_view_all_permission() {
        budgetRepository.summaries.add(summary(BUDGET_ID, OTHER_DEPARTMENT_ID));
        var useCase = new ListBudgetsUseCase(budgetRepository);

        var result = useCase.execute(listQuery(Set.of("BUDGET_VIEW_ALL"), OTHER_DEPARTMENT_ID));

        assertThat(result.items()).hasSize(1);
        assertThat(budgetRepository.lastFilter.departmentId()).isEqualTo(OTHER_DEPARTMENT_ID);
    }

    @Test
    void should_return_dashboard_from_cache_and_verify_scope() {
        cachePort.cached = BudgetDashboardView.from(summary(BUDGET_ID, OWN_DEPARTMENT_ID));
        var useCase = new GetBudgetDashboardUseCase(budgetRepository, cachePort);

        var result = useCase.execute(dashboardQuery(Set.of("BUDGET_VIEW_OWN_DEPT")));

        assertThat(result.id()).isEqualTo(BUDGET_ID);
        assertThat(budgetRepository.findSummaryByIdCalls).isZero();
    }

    @Test
    void should_throw_iam_004_when_cached_dashboard_is_outside_department_scope() {
        cachePort.cached = BudgetDashboardView.from(summary(BUDGET_ID, OTHER_DEPARTMENT_ID));
        var useCase = new GetBudgetDashboardUseCase(budgetRepository, cachePort);

        assertThatThrownBy(() -> useCase.execute(dashboardQuery(Set.of("BUDGET_VIEW_OWN_DEPT"))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IAM_004);
    }

    @Test
    void should_load_dashboard_from_repository_and_store_cache_when_cache_miss() {
        budgetRepository.summaryById = summary(BUDGET_ID, OWN_DEPARTMENT_ID);
        var useCase = new GetBudgetDashboardUseCase(budgetRepository, cachePort);

        var result = useCase.execute(dashboardQuery(Set.of("BUDGET_VIEW_OWN_DEPT")));

        assertThat(result.availablePercent()).isEqualByComparingTo("70.00");
        assertThat(cachePort.stored).isNotNull();
    }

    private static ListBudgetsQuery listQuery(Set<String> permissions, UUID requestedDepartmentId) {
        return new ListBudgetsQuery(
                ACTOR_ID,
                OWN_DEPARTMENT_ID,
                permissions,
                requestedDepartmentId,
                2026,
                null,
                "6002",
                BudgetStatus.ACTIVE,
                1,
                20,
                "available,asc");
    }

    private static GetBudgetDashboardQuery dashboardQuery(Set<String> permissions) {
        return new GetBudgetDashboardQuery(
                ACTOR_ID,
                OWN_DEPARTMENT_ID,
                permissions,
                BUDGET_ID);
    }

    private static BudgetLedgerSummary summary(UUID budgetId, UUID departmentId) {
        return new BudgetLedgerSummary(
                budgetId,
                departmentId,
                2026,
                null,
                "6002",
                money("500000000.0000"),
                money("100000000.0000"),
                money("50000000.0000"),
                BudgetStatus.ACTIVE);
    }

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount), "VND");
    }

    private static final class FakeBudgetDashboardCachePort implements BudgetDashboardCachePort {
        private BudgetDashboardView cached;
        private BudgetDashboardView stored;

        @Override
        public Optional<BudgetDashboardView> findByBudgetId(UUID budgetId) {
            return Optional.ofNullable(cached);
        }

        @Override
        public void store(BudgetDashboardView dashboard) {
            stored = dashboard;
        }

        @Override
        public void evict(UUID budgetId) {
        }
    }

    private static final class FakeBudgetRepository implements BudgetRepository {
        private final List<BudgetLedgerSummary> summaries = new ArrayList<>();
        private BudgetFilter lastFilter;
        private BudgetLedgerSummary summaryById;
        private int findSummaryByIdCalls;

        @Override
        public Optional<BudgetLedgerSummary> findActiveSummary(BudgetCheckCriteria criteria) {
            return Optional.empty();
        }

        @Override
        public List<BudgetLedgerSummary> findByFilter(BudgetFilter filter) {
            lastFilter = filter;
            return summaries;
        }

        @Override
        public long countByFilter(BudgetFilter filter) {
            return summaries.size();
        }

        @Override
        public Optional<BudgetLedgerSummary> findSummaryById(UUID budgetId) {
            findSummaryByIdCalls++;
            return Optional.ofNullable(summaryById);
        }

        @Override
        public boolean lockBudgetForUpdate(UUID budgetId) {
            return true;
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
        }

        @Override
        public Optional<BudgetCommitmentHold> findHeldCommitment(String referenceType, UUID referenceId) {
            return Optional.empty();
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
}
