package com.eprocure.analytics.presentation.response;

import com.eprocure.analytics.domain.model.KpiStatus;
import java.math.BigDecimal;

public record DepartmentSpendResponse(
        String departmentCode,
        String departmentName,
        String spent,
        String budget,
        BigDecimal utilization,
        KpiStatus status) {
}
