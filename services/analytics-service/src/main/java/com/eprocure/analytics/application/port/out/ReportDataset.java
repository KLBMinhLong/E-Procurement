package com.eprocure.analytics.application.port.out;

import java.util.List;

public record ReportDataset(
        List<ReportDatasetRow> rows) {

    public ReportDataset {
        rows = List.copyOf(rows == null ? List.of() : rows);
    }

    public static ReportDataset empty() {
        return new ReportDataset(List.of());
    }
}
