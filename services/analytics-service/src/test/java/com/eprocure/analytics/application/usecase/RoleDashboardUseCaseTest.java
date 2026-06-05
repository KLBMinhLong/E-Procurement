package com.eprocure.analytics.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.eprocure.analytics.application.port.in.GetRoleDashboardQuery;
import com.eprocure.analytics.domain.model.KpiCard;
import com.eprocure.analytics.domain.model.KpiStatus;
import com.eprocure.analytics.domain.model.dashboard.BudgetStatus;
import com.eprocure.analytics.domain.model.dashboard.DepartmentBudgetSummary;
import com.eprocure.analytics.domain.model.dashboard.ManagerDashboard;
import com.eprocure.analytics.domain.model.dashboard.MyPurchaseRequestStats;
import com.eprocure.analytics.domain.model.dashboard.PendingApprovals;
import com.eprocure.analytics.domain.model.dashboard.PoPipeline;
import com.eprocure.analytics.domain.model.dashboard.PurchasingDashboard;
import com.eprocure.analytics.domain.model.dashboard.RecentPurchaseRequest;
import com.eprocure.analytics.domain.model.dashboard.RequesterDashboard;
import com.eprocure.analytics.domain.model.dashboard.SlaWarning;
import com.eprocure.analytics.domain.model.dashboard.VendorPerformance;
import com.eprocure.analytics.domain.repository.ManagerDashboardRepository;
import com.eprocure.analytics.domain.repository.PurchasingDashboardRepository;
import com.eprocure.analytics.domain.repository.RequesterDashboardRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RoleDashboardUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-06-04T04:00:00Z");
    private static final UUID ACTOR_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final UUID DEPARTMENT_ID = UUID.fromString("40000000-0000-4000-8000-000000000001");

    @Test
    void should_return_manager_dashboard_from_projection_repository() {
        FakeManagerDashboardRepository repository = new FakeManagerDashboardRepository();
        repository.dashboard = new ManagerDashboard(
                List.of(
                        new KpiCard("analytics.kpi.submittedPrCount", "5", null, null, KpiStatus.GOOD),
                        new KpiCard("analytics.kpi.submittedPrTotal", "150000000", "VND", null, KpiStatus.GOOD),
                        new KpiCard("analytics.kpi.slaBreachCount", "2", null, null, KpiStatus.WARNING)),
                BudgetStatus.empty(),
                new PendingApprovals(5, 2, 0),
                List.of(new RecentPurchaseRequest("PR-2026-0001", "NORMAL", "SUBMITTED",
                        new BigDecimal("30000000"), "", null, NOW)),
                List.of(new SlaWarning("step-1", "PR-2026-0001", NOW, true)),
                NOW);
        var useCase = new GetManagerDashboardUseCase(Clock.fixed(NOW, ZoneOffset.UTC), repository);

        ManagerDashboard result = useCase.execute(query());

        assertThat(repository.departmentId).isEqualTo(DEPARTMENT_ID);
        assertThat(repository.cachedAt).isEqualTo(NOW);
        assertThat(result.kpis()).hasSize(3);
        assertThat(result.kpis()).extracting(KpiCard::label)
                .containsExactly("analytics.kpi.submittedPrCount", "analytics.kpi.submittedPrTotal", "analytics.kpi.slaBreachCount");
        assertThat(result.pendingApprovals().count()).isEqualTo(5);
        assertThat(result.pendingApprovals().overdueCount()).isEqualTo(2);
        assertThat(result.recentPrs()).hasSize(1);
        assertThat(result.recentPrs().get(0).prNumber()).isEqualTo("PR-2026-0001");
        assertThat(result.slaWarnings()).hasSize(1);
        assertThat(result.slaWarnings().get(0).overdue()).isTrue();
        assertThat(result.cachedAt()).isEqualTo(NOW);
    }

    @Test
    void should_return_empty_manager_dashboard_when_no_projections() {
        FakeManagerDashboardRepository repository = new FakeManagerDashboardRepository();
        var useCase = new GetManagerDashboardUseCase(Clock.fixed(NOW, ZoneOffset.UTC), repository);

        ManagerDashboard result = useCase.execute(query());

        assertThat(result.budgetStatus().available().signum()).isZero();
        assertThat(result.pendingApprovals().count()).isZero();
        assertThat(result.recentPrs()).isEmpty();
        assertThat(result.slaWarnings()).isEmpty();
        assertThat(result.cachedAt()).isEqualTo(NOW);
    }

    @Test
    void should_return_purchasing_dashboard_from_projection_repository() {
        FakePurchasingDashboardRepository repository = new FakePurchasingDashboardRepository();
        repository.dashboard = new PurchasingDashboard(
                List.of(new KpiCard("analytics.kpi.issuedPoCount", "2", null, null, KpiStatus.GOOD)),
                new PoPipeline(0, 0, 2, 0),
                0,
                0,
                0,
                List.of(new VendorPerformance("Acme Supplier", BigDecimal.ZERO, 0, 2)),
                NOW);
        var useCase = new GetPurchasingDashboardUseCase(Clock.fixed(NOW, ZoneOffset.UTC), repository);

        PurchasingDashboard result = useCase.execute(query());

        assertThat(repository.cachedAt).isEqualTo(NOW);
        assertThat(result.openRfqs()).isZero();
        assertThat(result.grPending()).isZero();
        assertThat(result.invoicesPendingMatch()).isZero();
        assertThat(result.poPipeline().sentToVendor()).isEqualTo(2);
        assertThat(result.vendorPerformance()).extracting(VendorPerformance::vendorName).containsExactly("Acme Supplier");
        assertThat(result.cachedAt()).isEqualTo(NOW);
    }

    @Test
    void should_return_requester_dashboard_from_projection_repository() {
        FakeRequesterDashboardRepository repository = new FakeRequesterDashboardRepository();
        repository.dashboard = new RequesterDashboard(
                new MyPurchaseRequestStats(0, 3, 0, 0, 0),
                DepartmentBudgetSummary.empty(),
                List.of(new RecentPurchaseRequest("PR-2026-0002", "HIGH", "SUBMITTED",
                        new BigDecimal("50000000"), "", null, NOW)),
                NOW);
        var useCase = new GetRequesterDashboardUseCase(Clock.fixed(NOW, ZoneOffset.UTC), repository);

        RequesterDashboard result = useCase.execute(query());

        assertThat(repository.requesterId).isEqualTo(ACTOR_ID);
        assertThat(repository.departmentId).isEqualTo(DEPARTMENT_ID);
        assertThat(result.myPrStats().pendingApproval()).isEqualTo(3);
        assertThat(result.recentPrs()).hasSize(1);
        assertThat(result.recentPrs().get(0).prNumber()).isEqualTo("PR-2026-0002");
        assertThat(result.departmentBudget().available().signum()).isZero();
        assertThat(result.cachedAt()).isEqualTo(NOW);
    }

    @Test
    void should_return_empty_requester_dashboard_when_no_projections() {
        FakeRequesterDashboardRepository repository = new FakeRequesterDashboardRepository();
        var useCase = new GetRequesterDashboardUseCase(Clock.fixed(NOW, ZoneOffset.UTC), repository);

        RequesterDashboard result = useCase.execute(query());

        assertThat(result.myPrStats().draft()).isZero();
        assertThat(result.departmentBudget().available().signum()).isZero();
        assertThat(result.recentPrs()).isEmpty();
        assertThat(result.cachedAt()).isEqualTo(NOW);
    }

    private GetRoleDashboardQuery query() {
        return new GetRoleDashboardQuery(ACTOR_ID, DEPARTMENT_ID);
    }

    private static final class FakeManagerDashboardRepository implements ManagerDashboardRepository {
        private UUID departmentId;
        private Instant cachedAt;
        private ManagerDashboard dashboard;

        @Override
        public ManagerDashboard findDashboard(UUID departmentId, Instant cachedAt) {
            this.departmentId = departmentId;
            this.cachedAt = cachedAt;
            return dashboard == null ? ManagerDashboard.empty(cachedAt) : dashboard;
        }
    }

    private static final class FakePurchasingDashboardRepository implements PurchasingDashboardRepository {
        private Instant cachedAt;
        private PurchasingDashboard dashboard;

        @Override
        public PurchasingDashboard findDashboard(Instant cachedAt) {
            this.cachedAt = cachedAt;
            return dashboard == null ? PurchasingDashboard.empty(cachedAt) : dashboard;
        }
    }

    private static final class FakeRequesterDashboardRepository implements RequesterDashboardRepository {
        private UUID requesterId;
        private UUID departmentId;
        private Instant cachedAt;
        private RequesterDashboard dashboard;

        @Override
        public RequesterDashboard findDashboard(UUID requesterId, UUID departmentId, Instant cachedAt) {
            this.requesterId = requesterId;
            this.departmentId = departmentId;
            this.cachedAt = cachedAt;
            return dashboard == null ? RequesterDashboard.empty(cachedAt) : dashboard;
        }
    }
}
