package com.eprocure.analytics.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eprocure.analytics.application.port.in.GetCycleTimeKpiQuery;
import com.eprocure.analytics.application.port.in.GetSlaComplianceKpiQuery;
import com.eprocure.analytics.common.exception.BusinessException;
import com.eprocure.analytics.common.exception.ErrorCode;
import com.eprocure.analytics.domain.model.kpi.ApproverRoleSla;
import com.eprocure.analytics.domain.model.kpi.CycleTimeKpi;
import com.eprocure.analytics.domain.model.kpi.PriorityCycleTime;
import com.eprocure.analytics.domain.model.kpi.SlaComplianceKpi;
import com.eprocure.analytics.domain.model.kpi.WeeklyCycleTime;
import com.eprocure.analytics.domain.model.kpi.WorstApproverSla;
import com.eprocure.analytics.domain.repository.KpiRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class KpiUseCaseTest {
    private static final UUID ACTOR_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final UUID DEPARTMENT_ID = UUID.fromString("40000000-0000-4000-8000-000000000001");
    private static final LocalDate FROM_DATE = LocalDate.of(2026, 6, 1);
    private static final LocalDate TO_DATE = LocalDate.of(2026, 6, 5);

    @Test
    void should_return_cycle_time_from_projection_repository() {
        FakeKpiRepository repository = new FakeKpiRepository();
        var useCase = new GetCycleTimeKpiUseCase(repository);

        var result = useCase.execute(new GetCycleTimeKpiQuery(ACTOR_ID, FROM_DATE, TO_DATE, DEPARTMENT_ID));

        assertThat(repository.cycleFromInclusive).isEqualTo(Instant.parse("2026-06-01T00:00:00Z"));
        assertThat(repository.cycleToExclusive).isEqualTo(Instant.parse("2026-06-06T00:00:00Z"));
        assertThat(repository.cycleDepartmentId).isEqualTo(DEPARTMENT_ID);
        assertThat(result.avgCycleHours()).isEqualByComparingTo("18.25");
        assertThat(result.medianCycleHours()).isEqualByComparingTo("16.00");
        assertThat(result.p95CycleHours()).isEqualByComparingTo("28.00");
        assertThat(result.target()).isEqualByComparingTo("48.00");
        assertThat(result.byPriority()).extracting(PriorityCycleTime::priority).containsExactly("NORMAL");
        assertThat(result.trend()).extracting(WeeklyCycleTime::week).containsExactly("2026-W23");
    }

    @Test
    void should_return_sla_compliance_from_projection_repository() {
        FakeKpiRepository repository = new FakeKpiRepository();
        var useCase = new GetSlaComplianceKpiUseCase(repository);

        var result = useCase.execute(new GetSlaComplianceKpiQuery(ACTOR_ID, FROM_DATE, TO_DATE));

        assertThat(repository.fromInclusive).isEqualTo(Instant.parse("2026-06-01T00:00:00Z"));
        assertThat(repository.toExclusive).isEqualTo(Instant.parse("2026-06-06T00:00:00Z"));
        // 10 assigned, 2 breached → (10-2)/10*100 = 80.00%
        assertThat(result.overallCompliancePct()).isEqualByComparingTo("80.00");
        assertThat(result.byApproverRole()).extracting(ApproverRoleSla::role).containsExactly("MANAGER");
        assertThat(result.byApproverRole().get(0).compliancePct()).isEqualByComparingTo("80.00");
        assertThat(result.byApproverRole().get(0).avgActionHours()).isEqualByComparingTo("26.50");
        assertThat(result.worstApprovers()).extracting(WorstApproverSla::approverName)
                .containsExactly("approver:30000000");
    }

    @Test
    void should_return_zero_compliance_when_no_steps_assigned() {
        FakeKpiRepository emptyRepository = new FakeKpiRepository() {
            @Override
            public SlaComplianceKpi findSlaCompliance(Instant fromInclusive, Instant toExclusive) {
                return new SlaComplianceKpi(BigDecimal.ZERO, List.of(), List.of());
            }
        };
        var useCase = new GetSlaComplianceKpiUseCase(emptyRepository);

        var result = useCase.execute(new GetSlaComplianceKpiQuery(ACTOR_ID, FROM_DATE, TO_DATE));

        assertThat(result.overallCompliancePct()).isEqualByComparingTo("0");
        assertThat(result.byApproverRole()).isEmpty();
        assertThat(result.worstApprovers()).isEmpty();
    }

    @Test
    void should_throw_val_001_when_cycle_time_from_date_after_to_date() {
        var useCase = new GetCycleTimeKpiUseCase(new FakeKpiRepository());

        assertThatThrownBy(() -> useCase.execute(new GetCycleTimeKpiQuery(
                        ACTOR_ID,
                        TO_DATE,
                        FROM_DATE,
                        DEPARTMENT_ID)))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.VAL_001);
    }

    @Test
    void should_throw_val_001_when_sla_from_date_after_to_date() {
        var useCase = new GetSlaComplianceKpiUseCase(new FakeKpiRepository());

        assertThatThrownBy(() -> useCase.execute(new GetSlaComplianceKpiQuery(ACTOR_ID, TO_DATE, FROM_DATE)))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.VAL_001);
    }

    private static class FakeKpiRepository implements KpiRepository {
        private Instant cycleFromInclusive;
        private Instant cycleToExclusive;
        private UUID cycleDepartmentId;
        private Instant fromInclusive;
        private Instant toExclusive;

        @Override
        public CycleTimeKpi findCycleTime(Instant fromInclusive, Instant toExclusive, UUID departmentId) {
            this.cycleFromInclusive = fromInclusive;
            this.cycleToExclusive = toExclusive;
            this.cycleDepartmentId = departmentId;
            return new CycleTimeKpi(
                    new BigDecimal("18.25"),
                    new BigDecimal("16.00"),
                    new BigDecimal("28.00"),
                    new BigDecimal("48.00"),
                    List.of(new PriorityCycleTime("NORMAL", new BigDecimal("18.25"))),
                    List.of(new WeeklyCycleTime("2026-W23", new BigDecimal("18.25"))));
        }

        @Override
        public SlaComplianceKpi findSlaCompliance(Instant fromInclusive, Instant toExclusive) {
            this.fromInclusive = fromInclusive;
            this.toExclusive = toExclusive;
            // Simulates 10 total assigned steps, 2 breached → 80% compliance
            return new SlaComplianceKpi(
                    new BigDecimal("80.00"),
                    List.of(new ApproverRoleSla("MANAGER", new BigDecimal("80.00"), new BigDecimal("26.50"), 2)),
                    List.of(new WorstApproverSla("approver:30000000", 2, new BigDecimal("80.00"))));
        }
    }
}
