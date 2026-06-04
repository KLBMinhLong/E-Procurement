package com.eprocure.analytics.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.eprocure.analytics.application.port.in.GetRoleDashboardQuery;
import com.eprocure.analytics.domain.model.KpiCard;
import com.eprocure.analytics.domain.model.KpiStatus;
import com.eprocure.analytics.domain.model.dashboard.ManagerDashboard;
import com.eprocure.analytics.domain.model.dashboard.PoPipeline;
import com.eprocure.analytics.domain.model.dashboard.PurchasingDashboard;
import com.eprocure.analytics.domain.model.dashboard.RequesterDashboard;
import com.eprocure.analytics.domain.model.dashboard.VendorPerformance;
import com.eprocure.analytics.domain.repository.PurchasingDashboardRepository;
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
    void should_return_empty_manager_dashboard_foundation() {
        var useCase = new GetManagerDashboardUseCase(Clock.fixed(NOW, ZoneOffset.UTC));

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
    void should_return_empty_requester_dashboard_foundation() {
        var useCase = new GetRequesterDashboardUseCase(Clock.fixed(NOW, ZoneOffset.UTC));

        RequesterDashboard result = useCase.execute(query());

        assertThat(result.myPrStats().draft()).isZero();
        assertThat(result.departmentBudget().available().signum()).isZero();
        assertThat(result.recentPrs()).isEmpty();
        assertThat(result.cachedAt()).isEqualTo(NOW);
    }

    private GetRoleDashboardQuery query() {
        return new GetRoleDashboardQuery(ACTOR_ID, DEPARTMENT_ID);
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
}
