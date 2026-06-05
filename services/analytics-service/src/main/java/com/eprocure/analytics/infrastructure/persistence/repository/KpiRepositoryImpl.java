package com.eprocure.analytics.infrastructure.persistence.repository;

import com.eprocure.analytics.domain.model.kpi.CycleTimeKpi;
import com.eprocure.analytics.domain.model.kpi.SlaComplianceKpi;
import com.eprocure.analytics.domain.repository.KpiRepository;
import com.eprocure.analytics.infrastructure.persistence.mapper.KpiMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class KpiRepositoryImpl implements KpiRepository {
    private static final BigDecimal DEFAULT_CYCLE_TIME_TARGET_HOURS = new BigDecimal("48.00");

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
        var byRole = mapper.findByApproverRole(fromInclusive, toExclusive).stream()
                .map(row -> row.toDomain())
                .toList();
        var worstApprovers = mapper.findWorstApprovers(fromInclusive, toExclusive).stream()
                .map(row -> row.toDomain())
                .toList();
        return new SlaComplianceKpi(BigDecimal.ZERO, byRole, worstApprovers);
    }
}
