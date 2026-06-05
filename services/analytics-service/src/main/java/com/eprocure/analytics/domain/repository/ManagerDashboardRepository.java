package com.eprocure.analytics.domain.repository;

import com.eprocure.analytics.domain.model.dashboard.ManagerDashboard;
import java.time.Instant;
import java.util.UUID;

public interface ManagerDashboardRepository {
    ManagerDashboard findDashboard(UUID departmentId, Instant cachedAt);
}
