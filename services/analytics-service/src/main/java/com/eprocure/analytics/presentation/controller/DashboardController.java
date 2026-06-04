package com.eprocure.analytics.presentation.controller;

import com.eprocure.analytics.application.usecase.GetExecutiveDashboardUseCase;
import com.eprocure.analytics.common.api.ApiResponse;
import com.eprocure.analytics.common.api.RequestIdUtil;
import com.eprocure.analytics.common.security.UserPrincipal;
import com.eprocure.analytics.common.util.LogMaskingUtil;
import com.eprocure.analytics.presentation.mapper.DashboardPresentationMapper;
import com.eprocure.analytics.presentation.response.ExecutiveDashboardResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
    private static final Logger log = LogManager.getLogger(DashboardController.class);

    private final GetExecutiveDashboardUseCase getExecutiveDashboardUseCase;
    private final DashboardPresentationMapper mapper;

    public DashboardController(
            GetExecutiveDashboardUseCase getExecutiveDashboardUseCase,
            DashboardPresentationMapper mapper) {
        this.getExecutiveDashboardUseCase = getExecutiveDashboardUseCase;
        this.mapper = mapper;
    }

    @GetMapping("/executive")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ResponseEntity<ApiResponse<ExecutiveDashboardResponse>> getExecutiveDashboard(
            @RequestParam(value = "fiscal_year", required = false) Integer fiscalYear,
            @RequestParam(value = "quarter", required = false) Integer quarter,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/dashboard/executive | userId={} | fiscalYear={} | quarter={}",
                LogMaskingUtil.maskId(principal.getId()),
                fiscalYear,
                quarter);
        var dashboard = getExecutiveDashboardUseCase.execute(mapper.toQuery(principal, fiscalYear, quarter));
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(dashboard), RequestIdUtil.resolve(request)));
    }
}
