package com.eprocure.analytics.infrastructure.persistence.repository;

import com.eprocure.analytics.domain.model.dashboard.DepartmentBudgetSummary;
import com.eprocure.analytics.domain.model.dashboard.RequesterDashboard;
import com.eprocure.analytics.domain.repository.RequesterDashboardRepository;
import com.eprocure.analytics.infrastructure.persistence.entity.RequesterPrStatsDbEntity;
import com.eprocure.analytics.infrastructure.persistence.mapper.RequesterDashboardMapper;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class RequesterDashboardRepositoryImpl implements RequesterDashboardRepository {
    private final RequesterDashboardMapper mapper;

    public RequesterDashboardRepositoryImpl(RequesterDashboardMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public RequesterDashboard findDashboard(UUID requesterId, UUID departmentId, Instant cachedAt) {
        RequesterPrStatsDbEntity stats = mapper.findPrStats(requesterId)
                .orElseGet(RequesterPrStatsDbEntity::new);
        return new RequesterDashboard(
                stats.toDomain(),
                DepartmentBudgetSummary.empty(),
                mapper.findRecentPrs(requesterId).stream()
                        .map(row -> row.toDomain())
                        .toList(),
                cachedAt);
    }
}
