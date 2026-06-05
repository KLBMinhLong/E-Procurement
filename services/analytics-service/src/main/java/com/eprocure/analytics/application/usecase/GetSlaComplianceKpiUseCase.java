package com.eprocure.analytics.application.usecase;

import com.eprocure.analytics.application.port.in.GetSlaComplianceKpiQuery;
import com.eprocure.analytics.common.exception.BusinessException;
import com.eprocure.analytics.common.exception.ErrorCode;
import com.eprocure.analytics.common.util.LogMaskingUtil;
import com.eprocure.analytics.domain.model.kpi.SlaComplianceKpi;
import com.eprocure.analytics.domain.repository.KpiRepository;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetSlaComplianceKpiUseCase {
    private static final Logger log = LogManager.getLogger(GetSlaComplianceKpiUseCase.class);

    private final KpiRepository repository;

    public GetSlaComplianceKpiUseCase(KpiRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public SlaComplianceKpi execute(GetSlaComplianceKpiQuery query) {
        validateDateRange(query);
        Instant fromInclusive = query.fromDate().atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toExclusive = query.toDate().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        log.info("[ACTION] Start GetSlaComplianceKpi | userId={} | fromDate={} | toDate={}",
                LogMaskingUtil.maskId(query.actorId()),
                query.fromDate(),
                query.toDate());
        SlaComplianceKpi kpi = repository.findSlaCompliance(fromInclusive, toExclusive);
        log.info("[ACTION] Complete GetSlaComplianceKpi | userId={} | roleRows={} | worstApprovers={}",
                LogMaskingUtil.maskId(query.actorId()),
                kpi.byApproverRole().size(),
                kpi.worstApprovers().size());
        return kpi;
    }

    private void validateDateRange(GetSlaComplianceKpiQuery query) {
        if (query.fromDate().isAfter(query.toDate())) {
            throw new BusinessException(ErrorCode.VAL_001, Map.of("from_date", "must be on or before to_date"));
        }
    }
}
