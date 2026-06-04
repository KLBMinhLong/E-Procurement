package com.eprocure.analytics.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.eprocure.analytics.application.port.in.GetRoleDashboardQuery;
import com.eprocure.analytics.domain.model.dashboard.ManagerDashboard;
import com.eprocure.analytics.domain.model.dashboard.PurchasingDashboard;
import com.eprocure.analytics.domain.model.dashboard.RequesterDashboard;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
    void should_return_empty_purchasing_dashboard_foundation() {
        var useCase = new GetPurchasingDashboardUseCase(Clock.fixed(NOW, ZoneOffset.UTC));

        PurchasingDashboard result = useCase.execute(query());

        assertThat(result.poPipeline().draft()).isZero();
        assertThat(result.openRfqs()).isZero();
        assertThat(result.grPending()).isZero();
        assertThat(result.invoicesPendingMatch()).isZero();
        assertThat(result.vendorPerformance()).isEmpty();
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
}
