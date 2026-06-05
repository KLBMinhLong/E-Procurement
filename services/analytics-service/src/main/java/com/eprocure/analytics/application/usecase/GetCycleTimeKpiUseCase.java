package com.eprocure.analytics.application.usecase;

import com.eprocure.analytics.application.port.in.GetCycleTimeKpiQuery;
import com.eprocure.analytics.common.exception.BusinessException;
import com.eprocure.analytics.common.exception.ErrorCode;
import com.eprocure.analytics.common.util.LogMaskingUtil;
import com.eprocure.analytics.domain.model.kpi.CycleTimeKpi;
import com.eprocure.analytics.domain.repository.KpiRepository;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetCycleTimeKpiUseCase {
    private static final Logger log = LogManager.getLogger(GetCycleTimeKpiUseCase.class);

    private final KpiRepository repository;

    public GetCycleTimeKpiUseCase(KpiRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public CycleTimeKpi execute(GetCycleTimeKpiQuery query) {
        validateDateRange(query);
        Instant fromInclusive = query.fromDate().atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toExclusive = query.toDate().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        log.info("[ACTION] Start GetCycleTimeKpi | userId={} | fromDate={} | toDate={} | departmentId={}",
                LogMaskingUtil.maskId(query.actorId()),
                query.fromDate(),
                query.toDate(),
                LogMaskingUtil.maskId(query.departmentId()));
        CycleTimeKpi kpi = repository.findCycleTime(fromInclusive, toExclusive, query.departmentId());
        log.info("[ACTION] Complete GetCycleTimeKpi | userId={} | priorityRows={} | trendRows={}",
                LogMaskingUtil.maskId(query.actorId()),
                kpi.byPriority().size(),
                kpi.trend().size());
        return kpi;
    }

    private void validateDateRange(GetCycleTimeKpiQuery query) {
        if (query.fromDate().isAfter(query.toDate())) {
            throw new BusinessException(ErrorCode.VAL_001, Map.of("from_date", "must be on or before to_date"));
        }
    }
}
