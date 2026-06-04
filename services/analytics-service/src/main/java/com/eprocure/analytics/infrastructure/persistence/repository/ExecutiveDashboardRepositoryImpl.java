package com.eprocure.analytics.infrastructure.persistence.repository;

import com.eprocure.analytics.domain.model.ExecutiveDashboard;
import com.eprocure.analytics.domain.repository.ExecutiveDashboardRepository;
import com.eprocure.analytics.infrastructure.persistence.entity.ExecutiveDashboardSnapshotDbEntity;
import com.eprocure.analytics.infrastructure.persistence.mapper.ExecutiveDashboardMapper;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ExecutiveDashboardRepositoryImpl implements ExecutiveDashboardRepository {
    private final ExecutiveDashboardMapper mapper;

    public ExecutiveDashboardRepositoryImpl(ExecutiveDashboardMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<ExecutiveDashboard> findLatest(int fiscalYear, Integer quarter, Instant cachedAfter) {
        Optional<ExecutiveDashboardSnapshotDbEntity> snapshot = mapper.findLatest(fiscalYear, quarter, cachedAfter);
        return snapshot.map(row -> row.toDomain(
                mapper.findDepartments(row.getId()),
                mapper.findCategories(row.getId()),
                mapper.findMonthlyTrend(row.getId()),
                mapper.findTopVendors(row.getId())));
    }
}
