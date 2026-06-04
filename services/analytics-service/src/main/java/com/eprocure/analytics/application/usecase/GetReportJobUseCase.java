package com.eprocure.analytics.application.usecase;

import com.eprocure.analytics.application.port.in.GetReportJobQuery;
import com.eprocure.analytics.common.exception.BusinessException;
import com.eprocure.analytics.common.exception.ErrorCode;
import com.eprocure.analytics.common.util.LogMaskingUtil;
import com.eprocure.analytics.domain.model.report.ReportJob;
import com.eprocure.analytics.domain.repository.ReportJobRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetReportJobUseCase {
    private static final Logger log = LogManager.getLogger(GetReportJobUseCase.class);

    private final ReportJobRepository repository;

    public GetReportJobUseCase(ReportJobRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public ReportJob execute(GetReportJobQuery query) {
        log.info("[ACTION] Start GetReportJob | userId={} | jobId={}",
                LogMaskingUtil.maskId(query.actorId()),
                LogMaskingUtil.maskId(query.jobId()));
        ReportJob job = repository.findByIdAndActorId(query.jobId(), query.actorId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ANL_002));
        log.info("[ACTION] Complete GetReportJob | userId={} | jobId={} | status={}",
                LogMaskingUtil.maskId(query.actorId()),
                LogMaskingUtil.maskId(query.jobId()),
                job.status());
        return job;
    }
}
