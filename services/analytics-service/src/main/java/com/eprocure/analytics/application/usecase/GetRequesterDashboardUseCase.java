package com.eprocure.analytics.application.usecase;

import com.eprocure.analytics.application.port.in.GetRoleDashboardQuery;
import com.eprocure.analytics.common.util.LogMaskingUtil;
import com.eprocure.analytics.domain.model.dashboard.RequesterDashboard;
import com.eprocure.analytics.domain.repository.RequesterDashboardRepository;
import java.time.Clock;
import java.time.Instant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetRequesterDashboardUseCase {
    private static final Logger log = LogManager.getLogger(GetRequesterDashboardUseCase.class);

    private final Clock clock;
    private final RequesterDashboardRepository repository;

    public GetRequesterDashboardUseCase(Clock clock, RequesterDashboardRepository repository) {
        this.clock = clock;
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public RequesterDashboard execute(GetRoleDashboardQuery query) {
        log.info("[ACTION] Start GetRequesterDashboard | userId={} | departmentId={}",
                LogMaskingUtil.maskId(query.actorId()),
                LogMaskingUtil.maskId(query.departmentId()));
        RequesterDashboard dashboard = repository.findDashboard(query.actorId(), query.departmentId(), Instant.now(clock));
        log.info("[ACTION] Complete GetRequesterDashboard | userId={} | cachedAt={}",
                LogMaskingUtil.maskId(query.actorId()),
                dashboard.cachedAt());
        return dashboard;
    }
}
