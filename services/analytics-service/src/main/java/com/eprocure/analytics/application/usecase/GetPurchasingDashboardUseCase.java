package com.eprocure.analytics.application.usecase;

import com.eprocure.analytics.application.port.in.GetRoleDashboardQuery;
import com.eprocure.analytics.common.util.LogMaskingUtil;
import com.eprocure.analytics.domain.model.dashboard.PurchasingDashboard;
import java.time.Clock;
import java.time.Instant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetPurchasingDashboardUseCase {
    private static final Logger log = LogManager.getLogger(GetPurchasingDashboardUseCase.class);

    private final Clock clock;

    public GetPurchasingDashboardUseCase(Clock clock) {
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PurchasingDashboard execute(GetRoleDashboardQuery query) {
        log.info("[ACTION] Start GetPurchasingDashboard | userId={} | departmentId={}",
                LogMaskingUtil.maskId(query.actorId()),
                LogMaskingUtil.maskId(query.departmentId()));
        PurchasingDashboard dashboard = PurchasingDashboard.empty(Instant.now(clock));
        log.info("[ACTION] Complete GetPurchasingDashboard | userId={} | cachedAt={}",
                LogMaskingUtil.maskId(query.actorId()),
                dashboard.cachedAt());
        return dashboard;
    }
}
