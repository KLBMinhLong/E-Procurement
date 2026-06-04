package com.eprocure.analytics.application.usecase;

import com.eprocure.analytics.application.port.in.GetReportJobQuery;
import com.eprocure.analytics.common.exception.BusinessException;
import com.eprocure.analytics.common.exception.ErrorCode;
import com.eprocure.analytics.domain.model.report.ReportJob;
import com.eprocure.analytics.domain.model.report.ReportJobStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetReportDownloadUseCase {
    private final GetReportJobUseCase getReportJobUseCase;

    public GetReportDownloadUseCase(GetReportJobUseCase getReportJobUseCase) {
        this.getReportJobUseCase = getReportJobUseCase;
    }

    @Transactional(readOnly = true)
    public ReportJob execute(GetReportJobQuery query) {
        ReportJob job = getReportJobUseCase.execute(query);
        if (job.status() != ReportJobStatus.COMPLETED || job.downloadUrl() == null) {
            throw new BusinessException(ErrorCode.ANL_003);
        }
        return job;
    }
}
