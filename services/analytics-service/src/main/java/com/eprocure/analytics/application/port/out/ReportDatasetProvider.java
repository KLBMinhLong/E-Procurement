package com.eprocure.analytics.application.port.out;

import com.eprocure.analytics.domain.model.report.ReportJob;

public interface ReportDatasetProvider {
    ReportDataset load(ReportJob job);
}
