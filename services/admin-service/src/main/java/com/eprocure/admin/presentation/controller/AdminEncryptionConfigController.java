package com.eprocure.admin.presentation.controller;

import com.eprocure.admin.application.usecase.RotateEncryptionKeyUseCase;
import com.eprocure.admin.common.api.ApiResponse;
import com.eprocure.admin.common.api.RequestIdUtil;
import com.eprocure.admin.common.security.UserPrincipal;
import com.eprocure.admin.common.util.LogMaskingUtil;
import com.eprocure.admin.presentation.mapper.AdminConfigPresentationMapper;
import com.eprocure.admin.presentation.request.EncryptionKeyRotationRequest;
import com.eprocure.admin.presentation.response.EncryptionKeyRotationResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/config/encryption")
public class AdminEncryptionConfigController {
    private static final Logger log = LogManager.getLogger(AdminEncryptionConfigController.class);

    private final RotateEncryptionKeyUseCase rotateEncryptionKeyUseCase;
    private final AdminConfigPresentationMapper mapper;

    public AdminEncryptionConfigController(
            RotateEncryptionKeyUseCase rotateEncryptionKeyUseCase,
            AdminConfigPresentationMapper mapper) {
        this.rotateEncryptionKeyUseCase = rotateEncryptionKeyUseCase;
        this.mapper = mapper;
    }

    @PostMapping("/rotate-key")
    @PreAuthorize("hasAuthority('SYSTEM_CONFIG')")
    public ResponseEntity<ApiResponse<EncryptionKeyRotationResponse>> rotateEncryptionKey(
            @Valid @RequestBody EncryptionKeyRotationRequest body,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        int keySize = body.keySize() == null ? 2048 : body.keySize();
        log.info("[CONTROLLER] POST /api/v1/admin/config/encryption/rotate-key | userId={} | keySize={}",
                LogMaskingUtil.maskId(principal.getId()),
                keySize);
        String requestId = RequestIdUtil.resolve(request);
        var result = rotateEncryptionKeyUseCase.execute(
                mapper.toCommand(body, principal, mapper.toAuditContext(principal, request, requestId)),
                idempotencyKey);
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        if (result.replayed()) {
            builder.header("Idempotency-Replayed", "true");
        }
        return builder.body(ApiResponse.success(mapper.toResponse(result), requestId));
    }
}
