package com.eprocure.analytics.domain.repository;

import com.eprocure.analytics.domain.model.ExecutiveDashboard;
import java.time.Instant;
import java.util.Optional;

public interface ExecutiveDashboardRepository {
    Optional<ExecutiveDashboard> findLatest(int fiscalYear, Integer quarter, Instant cachedAfter);
}
