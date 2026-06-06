package com.eprocure.analytics.domain.model.report;

import java.time.LocalDate;
import java.util.UUID;

public record ReportFilterCriteria(
        LocalDate fromDate,
        LocalDate toDate,
        Integer fiscalYear,
        Integer quarter,
        UUID vendorId,
        String categoryCode) {

    public ReportFilterCriteria {
        if (quarter != null && (quarter < 1 || quarter > 4)) {
            quarter = null;
        }
        categoryCode = categoryCode == null || categoryCode.isBlank() ? null : categoryCode.trim();
    }

    public static ReportFilterCriteria empty() {
        return new ReportFilterCriteria(null, null, null, null, null, null);
    }
}
