package com.eprocure.admin.presentation.controller;

import com.eprocure.admin.application.usecase.GetServiceConfigUseCase;
import com.eprocure.admin.application.usecase.ListServiceConfigsUseCase;
import com.eprocure.admin.application.usecase.RestartServiceUseCase;
import com.eprocure.admin.application.usecase.UpdateServiceConfigUseCase;
import com.eprocure.admin.common.api.ApiResponse;
import com.eprocure.admin.common.api.RequestIdUtil;
import com.eprocure.admin.common.security.UserPrincipal;
import com.eprocure.admin.common.util.LogMaskingUtil;
import com.eprocure.admin.presentation.mapper.AdminConfigPresentationMapper;
import com.eprocure.admin.presentation.request.ServiceConfigUpdateRequest;
import com.eprocure.admin.presentation.request.ServiceRestartRequest;
import com.eprocure.admin.presentation.response.ServiceConfigResponse;
import com.eprocure.admin.presentation.response.ServiceConfigUpdateResponse;
import com.eprocure.admin.presentation.response.ServiceRestartResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/config/services")
public class AdminConfigController {
    private static final Logger log = LogManager.getLogger(AdminConfigController.class);

    private final ListServiceConfigsUseCase listServiceConfigsUseCase;
    private final GetServiceConfigUseCase getServiceConfigUseCase;
    private final UpdateServiceConfigUseCase updateServiceConfigUseCase;
    private final RestartServiceUseCase restartServiceUseCase;
    private final AdminConfigPresentationMapper mapper;

    public AdminConfigController(
            ListServiceConfigsUseCase listServiceConfigsUseCase,
            GetServiceConfigUseCase getServiceConfigUseCase,
            UpdateServiceConfigUseCase updateServiceConfigUseCase,
            RestartServiceUseCase restartServiceUseCase,
            AdminConfigPresentationMapper mapper) {
        this.listServiceConfigsUseCase = listServiceConfigsUseCase;
        this.getServiceConfigUseCase = getServiceConfigUseCase;
        this.updateServiceConfigUseCase = updateServiceConfigUseCase;
        this.restartServiceUseCase = restartServiceUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SYSTEM_CONFIG')")
    public ResponseEntity<ApiResponse<List<ServiceConfigResponse>>> listServiceConfigs(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/admin/config/services | userId={}",
                LogMaskingUtil.maskId(principal.getId()));
        var configs = listServiceConfigsUseCase.execute(principal.getId());
        return ResponseEntity.ok(ApiResponse.successWithMeta(
                mapper.toResponseList(configs),
                mapper.toSummary(configs),
                RequestIdUtil.resolve(request)));
    }

    @GetMapping("/{serviceName}")
    @PreAuthorize("hasAuthority('SYSTEM_CONFIG')")
    public ResponseEntity<ApiResponse<ServiceConfigResponse>> getServiceConfig(
            @PathVariable String serviceName,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/admin/config/services/{} | userId={}",
                serviceName,
                LogMaskingUtil.maskId(principal.getId()));
        var config = getServiceConfigUseCase.execute(serviceName, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(config), RequestIdUtil.resolve(request)));
    }

    @PutMapping("/{serviceName}")
    @PreAuthorize("hasAuthority('SYSTEM_CONFIG')")
    public ResponseEntity<ApiResponse<ServiceConfigUpdateResponse>> updateServiceConfig(
            @PathVariable String serviceName,
            @Valid @RequestBody ServiceConfigUpdateRequest body,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] PUT /api/v1/admin/config/services/{} | userId={} | variableCount={} | requiresRestart={}",
                serviceName,
                LogMaskingUtil.maskId(principal.getId()),
                body.variables().size(),
                body.requiresRestart());
        var result = updateServiceConfigUseCase.execute(mapper.toCommand(serviceName, body, principal), idempotencyKey);
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        if (result.replayed()) {
            builder.header("Idempotency-Replayed", "true");
        }
        return builder.body(ApiResponse.success(mapper.toResponse(result), RequestIdUtil.resolve(request)));
    }

    @PostMapping("/{serviceName}/restart")
    @PreAuthorize("hasAuthority('SYSTEM_CONFIG')")
    public ResponseEntity<ApiResponse<ServiceRestartResponse>> restartService(
            @PathVariable String serviceName,
            @Valid @RequestBody ServiceRestartRequest body,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] POST /api/v1/admin/config/services/{}/restart | userId={}",
                serviceName,
                LogMaskingUtil.maskId(principal.getId()));
        var result = restartServiceUseCase.execute(mapper.toCommand(serviceName, body, principal), idempotencyKey);
        ResponseEntity.BodyBuilder builder = result.replayed()
                ? ResponseEntity.ok()
                : ResponseEntity.status(HttpStatus.ACCEPTED);
        if (result.replayed()) {
            builder.header("Idempotency-Replayed", "true");
        }
        return builder.body(ApiResponse.success(mapper.toResponse(result), RequestIdUtil.resolve(request)));
    }
}
