package com.eprocure.analytics.presentation.controller;

import com.eprocure.analytics.application.usecase.GetCycleTimeKpiUseCase;
import com.eprocure.analytics.application.usecase.GetSlaComplianceKpiUseCase;
import com.eprocure.analytics.common.api.ApiResponse;
import com.eprocure.analytics.common.api.RequestIdUtil;
import com.eprocure.analytics.common.security.UserPrincipal;
import com.eprocure.analytics.common.util.LogMaskingUtil;
import com.eprocure.analytics.presentation.mapper.KpiPresentationMapper;
import com.eprocure.analytics.presentation.response.CycleTimeKpiResponse;
import com.eprocure.analytics.presentation.response.SlaComplianceKpiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/kpi")
public class KpiController {
    private static final Logger log = LogManager.getLogger(KpiController.class);

    private final GetCycleTimeKpiUseCase getCycleTimeKpiUseCase;
    private final GetSlaComplianceKpiUseCase getSlaComplianceKpiUseCase;
    private final KpiPresentationMapper mapper;

    public KpiController(
            GetCycleTimeKpiUseCase getCycleTimeKpiUseCase,
            GetSlaComplianceKpiUseCase getSlaComplianceKpiUseCase,
            KpiPresentationMapper mapper) {
        this.getCycleTimeKpiUseCase = getCycleTimeKpiUseCase;
        this.getSlaComplianceKpiUseCase = getSlaComplianceKpiUseCase;
        this.mapper = mapper;
    }

    @GetMapping("/cycle-time")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ResponseEntity<ApiResponse<CycleTimeKpiResponse>> getCycleTimeKpi(
            @RequestParam("from_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam("to_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(value = "department_id", required = false) UUID departmentId,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/kpi/cycle-time | userId={} | fromDate={} | toDate={} | departmentId={}",
                LogMaskingUtil.maskId(principal.getId()),
                fromDate,
                toDate,
                LogMaskingUtil.maskId(departmentId));
        var kpi = getCycleTimeKpiUseCase.execute(mapper.toCycleTimeQuery(principal, fromDate, toDate, departmentId));
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(kpi), RequestIdUtil.resolve(request)));
    }

    @GetMapping("/sla-compliance")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ResponseEntity<ApiResponse<SlaComplianceKpiResponse>> getSlaComplianceKpi(
            @RequestParam("from_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam("to_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/kpi/sla-compliance | userId={} | fromDate={} | toDate={}",
                LogMaskingUtil.maskId(principal.getId()),
                fromDate,
                toDate);
        var kpi = getSlaComplianceKpiUseCase.execute(mapper.toSlaComplianceQuery(principal, fromDate, toDate));
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(kpi), RequestIdUtil.resolve(request)));
    }
}
