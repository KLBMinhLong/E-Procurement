package com.eprocure.analytics.application.usecase;

import com.eprocure.analytics.application.port.in.GetCycleTimeKpiQuery;
import com.eprocure.analytics.common.exception.BusinessException;
import com.eprocure.analytics.common.exception.ErrorCode;
import com.eprocure.analytics.common.util.LogMaskingUtil;
import com.eprocure.analytics.domain.model.kpi.CycleTimeKpi;
import java.math.BigDecimal;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetCycleTimeKpiUseCase {
    private static final Logger log = LogManager.getLogger(GetCycleTimeKpiUseCase.class);
    private static final BigDecimal DEFAULT_TARGET_HOURS = new BigDecimal("48.00");

    @Transactional(readOnly = true)
    public CycleTimeKpi execute(GetCycleTimeKpiQuery query) {
        validateDateRange(query);
        log.info("[ACTION] Start GetCycleTimeKpi | userId={} | fromDate={} | toDate={} | departmentId={}",
                LogMaskingUtil.maskId(query.actorId()),
                query.fromDate(),
                query.toDate(),
                LogMaskingUtil.maskId(query.departmentId()));
        CycleTimeKpi kpi = CycleTimeKpi.foundation(DEFAULT_TARGET_HOURS);
        log.info("[ACTION] Complete GetCycleTimeKpi | userId={} | source=foundation",
                LogMaskingUtil.maskId(query.actorId()));
        return kpi;
    }

    private void validateDateRange(GetCycleTimeKpiQuery query) {
        if (query.fromDate().isAfter(query.toDate())) {
            throw new BusinessException(ErrorCode.VAL_001, Map.of("from_date", "must be on or before to_date"));
        }
    }
}
