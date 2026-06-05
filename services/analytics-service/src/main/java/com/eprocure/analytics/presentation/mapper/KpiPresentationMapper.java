package com.eprocure.analytics.presentation.mapper;

import com.eprocure.analytics.application.port.in.GetCycleTimeKpiQuery;
import com.eprocure.analytics.application.port.in.GetSlaComplianceKpiQuery;
import com.eprocure.analytics.common.security.UserPrincipal;
import com.eprocure.analytics.domain.model.kpi.ApproverRoleSla;
import com.eprocure.analytics.domain.model.kpi.CycleTimeKpi;
import com.eprocure.analytics.domain.model.kpi.PriorityCycleTime;
import com.eprocure.analytics.domain.model.kpi.SlaComplianceKpi;
import com.eprocure.analytics.domain.model.kpi.WeeklyCycleTime;
import com.eprocure.analytics.domain.model.kpi.WorstApproverSla;
import com.eprocure.analytics.presentation.response.ApproverRoleSlaResponse;
import com.eprocure.analytics.presentation.response.CycleTimeKpiResponse;
import com.eprocure.analytics.presentation.response.PriorityCycleTimeResponse;
import com.eprocure.analytics.presentation.response.SlaComplianceKpiResponse;
import com.eprocure.analytics.presentation.response.WeeklyCycleTimeResponse;
import com.eprocure.analytics.presentation.response.WorstApproverSlaResponse;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class KpiPresentationMapper {
    public GetCycleTimeKpiQuery toCycleTimeQuery(
            UserPrincipal principal,
            LocalDate fromDate,
            LocalDate toDate,
            UUID departmentId) {
        return new GetCycleTimeKpiQuery(principal.getId(), fromDate, toDate, departmentId);
    }

    public GetSlaComplianceKpiQuery toSlaComplianceQuery(
            UserPrincipal principal,
            LocalDate fromDate,
            LocalDate toDate) {
        return new GetSlaComplianceKpiQuery(principal.getId(), fromDate, toDate);
    }

    public CycleTimeKpiResponse toResponse(CycleTimeKpi kpi) {
        return new CycleTimeKpiResponse(
                kpi.avgCycleHours(),
                kpi.medianCycleHours(),
                kpi.p95CycleHours(),
                kpi.target(),
                kpi.byPriority().stream().map(this::toResponse).toList(),
                kpi.trend().stream().map(this::toResponse).toList());
    }

    public SlaComplianceKpiResponse toResponse(SlaComplianceKpi kpi) {
        return new SlaComplianceKpiResponse(
                kpi.overallCompliancePct(),
                kpi.byApproverRole().stream().map(this::toResponse).toList(),
                kpi.worstApprovers().stream().map(this::toResponse).toList());
    }

    private PriorityCycleTimeResponse toResponse(PriorityCycleTime item) {
        return new PriorityCycleTimeResponse(item.priority(), item.avgHours());
    }

    private WeeklyCycleTimeResponse toResponse(WeeklyCycleTime item) {
        return new WeeklyCycleTimeResponse(item.week(), item.avgHours());
    }

    private ApproverRoleSlaResponse toResponse(ApproverRoleSla item) {
        return new ApproverRoleSlaResponse(item.role(), item.compliancePct(), item.avgActionHours(), item.overdueCount());
    }

    private WorstApproverSlaResponse toResponse(WorstApproverSla item) {
        return new WorstApproverSlaResponse(item.approverName(), item.overdueCount(), item.compliancePct());
    }
}
