package com.eprocure.analytics.application.usecase;

import com.eprocure.analytics.application.port.in.GetReportJobQuery;
import com.eprocure.analytics.common.exception.BusinessException;
import com.eprocure.analytics.common.exception.ErrorCode;
import com.eprocure.analytics.domain.model.report.ReportJob;
import com.eprocure.analytics.domain.model.report.ReportJobStatus;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetReportDownloadUseCase {
    private final GetReportJobUseCase getReportJobUseCase;
    private final Clock clock;

    public GetReportDownloadUseCase(GetReportJobUseCase getReportJobUseCase, Clock clock) {
        this.getReportJobUseCase = getReportJobUseCase;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ReportJob execute(GetReportJobQuery query) {
        ReportJob job = getReportJobUseCase.execute(query);
        if (job.status() != ReportJobStatus.COMPLETED
                || job.downloadUrl() == null
                || job.storagePath() == null
                || isExpired(job)) {
            throw new BusinessException(ErrorCode.ANL_003);
        }
        return job;
    }

    private boolean isExpired(ReportJob job) {
        return job.expiresAt() != null && job.expiresAt().isBefore(Instant.now(clock));
    }
}
