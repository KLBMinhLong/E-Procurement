package com.eprocure.analytics.application.usecase;

import com.eprocure.analytics.application.port.in.GetExecutiveDashboardQuery;
import com.eprocure.analytics.common.util.LogMaskingUtil;
import com.eprocure.analytics.domain.model.ExecutiveDashboard;
import com.eprocure.analytics.domain.repository.ExecutiveDashboardRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetExecutiveDashboardUseCase {
    private static final Logger log = LogManager.getLogger(GetExecutiveDashboardUseCase.class);

    private final ExecutiveDashboardRepository repository;
    private final Clock clock;
    private final long cacheTtlMinutes;

    public GetExecutiveDashboardUseCase(
            ExecutiveDashboardRepository repository,
            Clock clock,
            @Value("${eprocure.analytics.dashboard-cache-ttl-minutes:5}") long cacheTtlMinutes) {
        this.repository = repository;
        this.clock = clock;
        this.cacheTtlMinutes = cacheTtlMinutes;
    }

    @Transactional(readOnly = true)
    public ExecutiveDashboard execute(GetExecutiveDashboardQuery query) {
        Instant now = Instant.now(clock);
        Instant cachedAfter = now.minus(Math.max(cacheTtlMinutes, 1), ChronoUnit.MINUTES);
        log.info("[ACTION] Start GetExecutiveDashboard | userId={} | fiscalYear={} | quarter={}",
                LogMaskingUtil.maskId(query.actorId()),
                query.fiscalYear(),
                query.quarter());
        ExecutiveDashboard dashboard = repository.findLatest(query.fiscalYear(), query.quarter(), cachedAfter)
                .orElseGet(() -> ExecutiveDashboard.empty(query.fiscalYear(), query.quarter(), now));
        log.info("[ACTION] Complete GetExecutiveDashboard | userId={} | cachedAt={}",
                LogMaskingUtil.maskId(query.actorId()),
                dashboard.cachedAt());
        return dashboard;
    }
}
