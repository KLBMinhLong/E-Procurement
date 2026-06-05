package com.eprocure.analytics.domain.repository;

import com.eprocure.analytics.domain.model.kpi.CycleTimeKpi;
import com.eprocure.analytics.domain.model.kpi.SlaComplianceKpi;
import java.time.Instant;
import java.util.UUID;

public interface KpiRepository {
    CycleTimeKpi findCycleTime(Instant fromInclusive, Instant toExclusive, UUID departmentId);

    SlaComplianceKpi findSlaCompliance(Instant fromInclusive, Instant toExclusive);
}
