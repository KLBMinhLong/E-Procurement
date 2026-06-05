package com.eprocure.analytics.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eprocure.analytics.application.port.in.GetCycleTimeKpiQuery;
import com.eprocure.analytics.application.port.in.GetSlaComplianceKpiQuery;
import com.eprocure.analytics.common.exception.BusinessException;
import com.eprocure.analytics.common.exception.ErrorCode;
import com.eprocure.analytics.domain.model.kpi.ApproverRoleSla;
import com.eprocure.analytics.domain.model.kpi.SlaComplianceKpi;
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
    void should_return_cycle_time_foundation_when_projection_source_missing() {
        var useCase = new GetCycleTimeKpiUseCase();

        var result = useCase.execute(new GetCycleTimeKpiQuery(ACTOR_ID, FROM_DATE, TO_DATE, DEPARTMENT_ID));

        assertThat(result.avgCycleHours().signum()).isZero();
        assertThat(result.medianCycleHours().signum()).isZero();
        assertThat(result.p95CycleHours().signum()).isZero();
        assertThat(result.target()).isEqualByComparingTo("48.00");
        assertThat(result.byPriority()).isEmpty();
        assertThat(result.trend()).isEmpty();
    }

    @Test
    void should_return_sla_compliance_from_projection_repository() {
        FakeKpiRepository repository = new FakeKpiRepository();
        var useCase = new GetSlaComplianceKpiUseCase(repository);

        var result = useCase.execute(new GetSlaComplianceKpiQuery(ACTOR_ID, FROM_DATE, TO_DATE));

        assertThat(repository.fromInclusive).isEqualTo(Instant.parse("2026-06-01T00:00:00Z"));
        assertThat(repository.toExclusive).isEqualTo(Instant.parse("2026-06-06T00:00:00Z"));
        assertThat(result.overallCompliancePct().signum()).isZero();
        assertThat(result.byApproverRole()).extracting(ApproverRoleSla::role).containsExactly("MANAGER");
        assertThat(result.byApproverRole().get(0).avgActionHours()).isEqualByComparingTo("26.50");
        assertThat(result.worstApprovers()).extracting(WorstApproverSla::approverName)
                .containsExactly("approver:30000000");
    }

    @Test
    void should_throw_val_001_when_cycle_time_from_date_after_to_date() {
        var useCase = new GetCycleTimeKpiUseCase();

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

    private static final class FakeKpiRepository implements KpiRepository {
        private Instant fromInclusive;
        private Instant toExclusive;

        @Override
        public SlaComplianceKpi findSlaCompliance(Instant fromInclusive, Instant toExclusive) {
            this.fromInclusive = fromInclusive;
            this.toExclusive = toExclusive;
            return new SlaComplianceKpi(
                    BigDecimal.ZERO,
                    List.of(new ApproverRoleSla("MANAGER", BigDecimal.ZERO, new BigDecimal("26.50"), 2)),
                    List.of(new WorstApproverSla("approver:30000000", 2, BigDecimal.ZERO)));
        }
    }
}
