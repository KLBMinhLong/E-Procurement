package com.eprocure.analytics.infrastructure.persistence.entity;

import com.eprocure.analytics.application.port.out.ReportDatasetRow;

public class ReportDatasetRowDbEntity {
    private String label;
    private String value;

    public ReportDatasetRow toDomain() {
        return new ReportDatasetRow(label, value);
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
