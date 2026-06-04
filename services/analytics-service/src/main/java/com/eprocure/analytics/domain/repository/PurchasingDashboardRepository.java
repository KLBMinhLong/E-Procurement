package com.eprocure.analytics.domain.repository;

import com.eprocure.analytics.domain.model.dashboard.PurchasingDashboard;
import java.time.Instant;

public interface PurchasingDashboardRepository {
    PurchasingDashboard findDashboard(Instant cachedAt);
}
