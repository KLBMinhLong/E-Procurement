package com.eprocure.analytics.domain.repository;

import com.eprocure.analytics.domain.model.kpi.SlaComplianceKpi;
import java.time.Instant;

public interface KpiRepository {
    SlaComplianceKpi findSlaCompliance(Instant fromInclusive, Instant toExclusive);
}
