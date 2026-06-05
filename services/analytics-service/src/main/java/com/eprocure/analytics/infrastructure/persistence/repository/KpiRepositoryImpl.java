package com.eprocure.analytics.infrastructure.persistence.repository;

import com.eprocure.analytics.domain.model.kpi.CycleTimeKpi;
import com.eprocure.analytics.domain.model.kpi.SlaComplianceKpi;
import com.eprocure.analytics.domain.repository.KpiRepository;
import com.eprocure.analytics.infrastructure.persistence.mapper.KpiMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class KpiRepositoryImpl implements KpiRepository {
    private static final BigDecimal DEFAULT_CYCLE_TIME_TARGET_HOURS = new BigDecimal("48.00");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final KpiMapper mapper;

    public KpiRepositoryImpl(KpiMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public CycleTimeKpi findCycleTime(Instant fromInclusive, Instant toExclusive, UUID departmentId) {
        var summary = mapper.findCycleTimeSummary(fromInclusive, toExclusive, departmentId);
        var byPriority = mapper.findCycleTimeByPriority(fromInclusive, toExclusive, departmentId).stream()
                .map(row -> row.toDomain())
                .toList();
        var trend = mapper.findCycleTimeTrend(fromInclusive, toExclusive, departmentId).stream()
                .map(row -> row.toDomain())
                .toList();
        return new CycleTimeKpi(
                summary == null ? BigDecimal.ZERO : summary.getAvgCycleHours(),
                summary == null ? BigDecimal.ZERO : summary.getMedianCycleHours(),
                summary == null ? BigDecimal.ZERO : summary.getP95CycleHours(),
                DEFAULT_CYCLE_TIME_TARGET_HOURS,
                byPriority,
                trend);
    }

    @Override
    public SlaComplianceKpi findSlaCompliance(Instant fromInclusive, Instant toExclusive) {
        int totalAssigned = mapper.countTotalAssignedSteps(fromInclusive, toExclusive);
        int totalBreached = mapper.countBreachedSteps(fromInclusive, toExclusive);

        BigDecimal overallCompliancePct = computeCompliancePct(totalAssigned, totalBreached);

        var byRole = mapper.findByApproverRole(fromInclusive, toExclusive).stream()
                .map(row -> row.toDomain())
                .toList();
        var worstApprovers = mapper.findWorstApprovers(fromInclusive, toExclusive).stream()
                .map(row -> row.toDomain())
                .toList();
        return new SlaComplianceKpi(overallCompliancePct, byRole, worstApprovers);
    }

    /**
     * Computes SLA compliance percentage: (total - breached) / total * 100.
     * Returns 0 when no steps have been assigned in the period.
     */
    private BigDecimal computeCompliancePct(int totalAssigned, int totalBreached) {
        if (totalAssigned == 0) {
            return BigDecimal.ZERO;
        }
        int onTime = totalAssigned - totalBreached;
        return BigDecimal.valueOf(onTime)
                .multiply(ONE_HUNDRED)
                .divide(BigDecimal.valueOf(totalAssigned), 2, RoundingMode.HALF_UP);
    }
}
