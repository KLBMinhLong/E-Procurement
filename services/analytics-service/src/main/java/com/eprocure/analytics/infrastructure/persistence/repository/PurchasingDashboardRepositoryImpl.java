package com.eprocure.analytics.infrastructure.persistence.repository;

import com.eprocure.analytics.domain.model.dashboard.PurchasingDashboard;
import com.eprocure.analytics.domain.repository.PurchasingDashboardRepository;
import com.eprocure.analytics.infrastructure.persistence.entity.PurchasingDashboardMetricsDbEntity;
import com.eprocure.analytics.infrastructure.persistence.mapper.PurchasingDashboardMapper;
import java.time.Instant;
import org.springframework.stereotype.Repository;

@Repository
public class PurchasingDashboardRepositoryImpl implements PurchasingDashboardRepository {
    private final PurchasingDashboardMapper mapper;

    public PurchasingDashboardRepositoryImpl(PurchasingDashboardMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public PurchasingDashboard findDashboard(Instant cachedAt) {
        PurchasingDashboardMetricsDbEntity metrics = mapper.findMetrics()
                .orElseGet(PurchasingDashboardMetricsDbEntity::new);
        return metrics.toDomain(
                cachedAt,
                mapper.findVendorPerformance().stream()
                        .map(row -> row.toDomain())
                        .toList());
    }
}
