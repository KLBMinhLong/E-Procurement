package com.eprocure.analytics.application.port.out;

import com.eprocure.analytics.domain.model.report.ReportJob;

public interface ReportFileRenderer {
    RenderedReport render(ReportJob job, ReportDataset dataset);
}
