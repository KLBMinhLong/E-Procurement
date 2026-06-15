package com.eprocure.admin.presentation.controller;

import com.eprocure.admin.application.usecase.GetSystemHealthUseCase;
import com.eprocure.admin.common.api.ApiResponse;
import com.eprocure.admin.common.api.RequestIdUtil;
import com.eprocure.admin.common.security.UserPrincipal;
import com.eprocure.admin.common.util.LogMaskingUtil;
import com.eprocure.admin.presentation.mapper.AdminHealthPresentationMapper;
import com.eprocure.admin.presentation.response.SystemHealthResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/health")
public class AdminHealthController {
    private static final Logger log = LogManager.getLogger(AdminHealthController.class);

    private final GetSystemHealthUseCase getSystemHealthUseCase;
    private final AdminHealthPresentationMapper mapper;

    public AdminHealthController(
            GetSystemHealthUseCase getSystemHealthUseCase,
            AdminHealthPresentationMapper mapper) {
        this.getSystemHealthUseCase = getSystemHealthUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SYSTEM_CONFIG')")
    public ResponseEntity<ApiResponse<SystemHealthResponse>> getSystemHealth(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/admin/health | userId={}",
                LogMaskingUtil.maskId(principal.getId()));
        var health = getSystemHealthUseCase.execute(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(health), RequestIdUtil.resolve(request)));
    }
}
