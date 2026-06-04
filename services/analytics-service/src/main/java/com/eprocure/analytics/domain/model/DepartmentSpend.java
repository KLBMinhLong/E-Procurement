package com.eprocure.analytics.domain.model;

import java.math.BigDecimal;

public record DepartmentSpend(
        String departmentCode,
        String departmentName,
        BigDecimal spent,
        BigDecimal budget,
        BigDecimal utilization,
        KpiStatus status) {

    public DepartmentSpend {
        departmentCode = requireText(departmentCode, "departmentCode");
        departmentName = requireText(departmentName, "departmentName");
        spent = spent == null ? BigDecimal.ZERO : spent;
        budget = budget == null ? BigDecimal.ZERO : budget;
        utilization = utilization == null ? BigDecimal.ZERO : utilization;
        status = status == null ? KpiStatus.GOOD : status;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
