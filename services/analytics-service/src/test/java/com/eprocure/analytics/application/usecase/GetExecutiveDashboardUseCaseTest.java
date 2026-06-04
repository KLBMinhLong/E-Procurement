package com.eprocure.analytics.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.eprocure.analytics.application.port.in.GetExecutiveDashboardQuery;
import com.eprocure.analytics.domain.model.ApprovalSla;
import com.eprocure.analytics.domain.model.ChartDataPoint;
import com.eprocure.analytics.domain.model.DepartmentSpend;
import com.eprocure.analytics.domain.model.ExecutiveDashboard;
import com.eprocure.analytics.domain.model.KpiCard;
import com.eprocure.analytics.domain.model.KpiStatus;
import com.eprocure.analytics.domain.model.MonthlyTrend;
import com.eprocure.analytics.domain.model.TopVendor;
import com.eprocure.analytics.domain.repository.ExecutiveDashboardRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetExecutiveDashboardUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-06-04T04:00:00Z");
    private static final UUID ACTOR_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");

    @Test
    void should_return_latest_snapshot_when_available() {
        FakeExecutiveDashboardRepository repository = new FakeExecutiveDashboardRepository();
        repository.dashboard = dashboard();
        var useCase = new GetExecutiveDashboardUseCase(repository, Clock.fixed(NOW, ZoneOffset.UTC), 5);

        ExecutiveDashboard result = useCase.execute(new GetExecutiveDashboardQuery(ACTOR_ID, 2026, null));

        assertThat(result.id()).isEqualTo(repository.dashboard.id());
        assertThat(result.kpis()).hasSize(3);
        assertThat(result.spendByDepartment()).hasSize(1);
        assertThat(repository.cachedAfter).isEqualTo(NOW.minusSeconds(300));
    }

    @Test
    void should_return_empty_dashboard_when_snapshot_missing() {
        FakeExecutiveDashboardRepository repository = new FakeExecutiveDashboardRepository();
        var useCase = new GetExecutiveDashboardUseCase(repository, Clock.fixed(NOW, ZoneOffset.UTC), 5);

        ExecutiveDashboard result = useCase.execute(new GetExecutiveDashboardQuery(ACTOR_ID, 2026, 2));

        assertThat(result.fiscalYear()).isEqualTo(2026);
        assertThat(result.quarter()).isEqualTo(2);
        assertThat(result.kpis()).extracting(KpiCard::value).containsExactly("0.0000", "0", "0.0000");
        assertThat(result.spendByDepartment()).isEmpty();
        assertThat(result.cachedAt()).isEqualTo(NOW);
    }

    private static ExecutiveDashboard dashboard() {
        return new ExecutiveDashboard(
                UUID.fromString("70000000-0000-4000-8000-000000000001"),
                2026,
                null,
                "VND",
                List.of(
                        new KpiCard("analytics.kpi.totalSpendYtd", "1000.0000", "VND", null, KpiStatus.GOOD),
                        new KpiCard("analytics.kpi.approvedPrCount", "5", null, null, KpiStatus.GOOD),
                        new KpiCard("analytics.kpi.rfqSavings", "100.0000", "VND", null, KpiStatus.GOOD)),
                List.of(new DepartmentSpend(
                        "IT",
                        "Information Technology",
                        new BigDecimal("1000.0000"),
                        new BigDecimal("2000.0000"),
                        new BigDecimal("50.00"),
                        KpiStatus.GOOD)),
                List.of(new ChartDataPoint("IT_HARDWARE", new BigDecimal("1000.0000"), null)),
                List.of(new MonthlyTrend("2026-06", new BigDecimal("1000.0000"), new BigDecimal("2000.0000"), 5)),
                new ApprovalSla(new BigDecimal("90.00"), new BigDecimal("12.00"), 1),
                List.of(new TopVendor("Acme Supplier", new BigDecimal("1000.0000"), 2, new BigDecimal("4.50"))),
                NOW.minusSeconds(60));
    }

    private static final class FakeExecutiveDashboardRepository implements ExecutiveDashboardRepository {
        private ExecutiveDashboard dashboard;
        private Instant cachedAfter;

        @Override
        public Optional<ExecutiveDashboard> findLatest(int fiscalYear, Integer quarter, Instant cachedAfter) {
            this.cachedAfter = cachedAfter;
            return Optional.ofNullable(dashboard)
                    .filter(value -> value.fiscalYear() == fiscalYear)
                    .filter(value -> value.quarter() == quarter || value.quarter() != null && value.quarter().equals(quarter));
        }
    }
}
