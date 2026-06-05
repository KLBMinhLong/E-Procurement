package com.eprocure.analytics.infrastructure.persistence.repository;

import com.eprocure.analytics.domain.model.kpi.SlaComplianceKpi;
import com.eprocure.analytics.domain.repository.KpiRepository;
import com.eprocure.analytics.infrastructure.persistence.mapper.KpiMapper;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.stereotype.Repository;

@Repository
public class KpiRepositoryImpl implements KpiRepository {
    private final KpiMapper mapper;

    public KpiRepositoryImpl(KpiMapper mapper) {
        this.mapper = mapper;
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
