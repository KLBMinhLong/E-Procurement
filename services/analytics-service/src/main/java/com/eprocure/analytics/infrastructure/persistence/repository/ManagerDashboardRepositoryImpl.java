package com.eprocure.analytics.infrastructure.persistence.repository;

import com.eprocure.analytics.domain.model.dashboard.ManagerDashboard;
import com.eprocure.analytics.domain.repository.ManagerDashboardRepository;
import com.eprocure.analytics.infrastructure.persistence.entity.ManagerDashboardMetricsDbEntity;
import com.eprocure.analytics.infrastructure.persistence.mapper.ManagerDashboardMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class ManagerDashboardRepositoryImpl implements ManagerDashboardRepository {
    private final ManagerDashboardMapper mapper;

    public ManagerDashboardRepositoryImpl(ManagerDashboardMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ManagerDashboard findDashboard(UUID departmentId, Instant cachedAt) {
        ManagerDashboardMetricsDbEntity metrics = mapper.findMetrics(departmentId)
                .orElseGet(ManagerDashboardMetricsDbEntity::new);
        return new ManagerDashboard(
                List.of(
                        metrics.submittedPrCountKpi(),
                        metrics.submittedPrTotalKpi(),
                        metrics.slaBreachCountKpi()),
                metrics.toBudgetStatus(),
                metrics.toPendingApprovals(),
                mapper.findRecentPrs(departmentId).stream()
                        .map(row -> row.toDomain())
                        .toList(),
                mapper.findSlaWarnings().stream()
                        .map(row -> row.toDomain())
                        .toList(),
                cachedAt);
    }
}
