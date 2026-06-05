package com.eprocure.analytics.application.port.out;

public record ReportDatasetRow(
        String label,
        String value) {

    public ReportDatasetRow {
        label = label == null || label.isBlank() ? "N/A" : label.trim();
        value = value == null || value.isBlank() ? "N/A" : value.trim();
    }
}
