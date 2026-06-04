package com.eprocure.analytics.application.service;

import com.eprocure.analytics.domain.model.report.ReportJob;

public record ReportJobMutationResult(
        ReportJob job,
        boolean replayed) {
}
