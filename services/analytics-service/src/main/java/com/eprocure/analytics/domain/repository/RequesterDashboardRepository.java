package com.eprocure.analytics.domain.repository;

import com.eprocure.analytics.domain.model.dashboard.RequesterDashboard;
import java.time.Instant;
import java.util.UUID;

public interface RequesterDashboardRepository {
    RequesterDashboard findDashboard(UUID requesterId, UUID departmentId, Instant cachedAt);
}
