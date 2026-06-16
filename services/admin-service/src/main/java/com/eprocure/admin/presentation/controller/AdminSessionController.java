package com.eprocure.admin.presentation.controller;

import com.eprocure.admin.application.usecase.InvalidateSessionUseCase;
import com.eprocure.admin.application.usecase.ListActiveSessionsUseCase;
import com.eprocure.admin.common.api.ApiResponse;
import com.eprocure.admin.common.api.RequestIdUtil;
import com.eprocure.admin.common.security.UserPrincipal;
import com.eprocure.admin.common.util.LogMaskingUtil;
import com.eprocure.admin.presentation.mapper.AdminSessionPresentationMapper;
import com.eprocure.admin.presentation.request.InvalidateSessionRequest;
import com.eprocure.admin.presentation.response.ActiveSessionResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/sessions")
public class AdminSessionController {
    private static final Logger log = LogManager.getLogger(AdminSessionController.class);

    private final ListActiveSessionsUseCase listActiveSessionsUseCase;
    private final InvalidateSessionUseCase invalidateSessionUseCase;
    private final AdminSessionPresentationMapper mapper;

    public AdminSessionController(
            ListActiveSessionsUseCase listActiveSessionsUseCase,
            InvalidateSessionUseCase invalidateSessionUseCase,
            AdminSessionPresentationMapper mapper) {
        this.listActiveSessionsUseCase = listActiveSessionsUseCase;
        this.invalidateSessionUseCase = invalidateSessionUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SYSTEM_CONFIG')")
    public ResponseEntity<ApiResponse<List<ActiveSessionResponse>>> listActiveSessions(
            @RequestParam(name = "user_id", required = false) UUID userId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/admin/sessions | userId={} | filterUserId={}",
                LogMaskingUtil.maskId(principal.getId()),
                LogMaskingUtil.maskId(userId));
        var result = listActiveSessionsUseCase.execute(mapper.toQuery(userId, page, size));
        return ResponseEntity.ok(ApiResponse.successWithMeta(
                mapper.toResponseList(result.items()),
                result.meta(),
                RequestIdUtil.resolve(request)));
    }

    @PatchMapping("/{sessionId}/invalidate")
    @PreAuthorize("hasAuthority('SYSTEM_CONFIG')")
    public ResponseEntity<ApiResponse<Void>> invalidateSession(
            @PathVariable UUID sessionId,
            @Valid @RequestBody InvalidateSessionRequest body,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] PATCH /api/v1/admin/sessions/{}/invalidate | userId={}",
                LogMaskingUtil.maskId(sessionId),
                LogMaskingUtil.maskId(principal.getId()));
        String requestId = RequestIdUtil.resolve(request);
        invalidateSessionUseCase.execute(
                mapper.toCommand(sessionId, body, principal, mapper.toAuditContext(principal, request, requestId)),
                idempotencyKey);
        return ResponseEntity.ok(ApiResponse.<Void>success(null, requestId));
    }
}
