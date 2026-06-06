package com.eprocure.analytics.application.usecase;

import com.eprocure.analytics.application.port.in.GetRoleDashboardQuery;
import com.eprocure.analytics.common.util.LogMaskingUtil;
import com.eprocure.analytics.domain.model.dashboard.ManagerDashboard;
import com.eprocure.analytics.domain.repository.ManagerDashboardRepository;
import java.time.Clock;
import java.time.Instant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetManagerDashboardUseCase {
    private static final Logger log = LogManager.getLogger(GetManagerDashboardUseCase.class);

    private final Clock clock;
    private final ManagerDashboardRepository repository;

    public GetManagerDashboardUseCase(Clock clock, ManagerDashboardRepository repository) {
        this.clock = clock;
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public ManagerDashboard execute(GetRoleDashboardQuery query) {
        log.info("[ACTION] Start GetManagerDashboard | userId={} | departmentId={}",
                LogMaskingUtil.maskId(query.actorId()),
                LogMaskingUtil.maskId(query.departmentId()));
        ManagerDashboard dashboard = repository.findDashboard(query.departmentId(), Instant.now(clock));
        log.info("[ACTION] Complete GetManagerDashboard | userId={} | cachedAt={}",
                LogMaskingUtil.maskId(query.actorId()),
                dashboard.cachedAt());
        return dashboard;
    }
}
