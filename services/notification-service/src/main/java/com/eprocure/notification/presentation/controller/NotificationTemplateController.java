package com.eprocure.notification.presentation.controller;

import com.eprocure.notification.application.service.NotificationTemplatePreviewResult;
import com.eprocure.notification.application.service.NotificationTemplateUpdateResult;
import com.eprocure.notification.application.service.NotificationTemplateView;
import com.eprocure.notification.application.usecase.ListNotificationTemplatesUseCase;
import com.eprocure.notification.application.usecase.PreviewNotificationTemplateUseCase;
import com.eprocure.notification.application.usecase.UpdateNotificationTemplateUseCase;
import com.eprocure.notification.common.api.ApiResponse;
import com.eprocure.notification.common.api.RequestIdUtil;
import com.eprocure.notification.common.security.UserPrincipal;
import com.eprocure.notification.common.util.LogMaskingUtil;
import com.eprocure.notification.presentation.mapper.NotificationTemplatePresentationMapper;
import com.eprocure.notification.presentation.request.UpdateNotificationTemplateRequest;
import com.eprocure.notification.presentation.response.NotificationTemplatePreviewResponse;
import com.eprocure.notification.presentation.response.NotificationTemplateResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notification-templates")
public class NotificationTemplateController {
    private static final Logger log = LogManager.getLogger(NotificationTemplateController.class);

    private final ListNotificationTemplatesUseCase listNotificationTemplatesUseCase;
    private final UpdateNotificationTemplateUseCase updateNotificationTemplateUseCase;
    private final PreviewNotificationTemplateUseCase previewNotificationTemplateUseCase;
    private final NotificationTemplatePresentationMapper mapper;

    public NotificationTemplateController(
            ListNotificationTemplatesUseCase listNotificationTemplatesUseCase,
            UpdateNotificationTemplateUseCase updateNotificationTemplateUseCase,
            PreviewNotificationTemplateUseCase previewNotificationTemplateUseCase,
            NotificationTemplatePresentationMapper mapper) {
        this.listNotificationTemplatesUseCase = listNotificationTemplatesUseCase;
        this.updateNotificationTemplateUseCase = updateNotificationTemplateUseCase;
        this.previewNotificationTemplateUseCase = previewNotificationTemplateUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SYSTEM_CONFIG')")
    public ResponseEntity<ApiResponse<List<NotificationTemplateResponse>>> list(
            @RequestParam(value = "channel", required = false) String channel,
            @RequestParam(value = "event_type", required = false) String eventType,
            @RequestParam(value = "language", required = false) String language,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/notification-templates | userId={}",
                LogMaskingUtil.maskId(principal.getId()));
        List<NotificationTemplateResponse> data = listNotificationTemplatesUseCase
                .execute(mapper.toListQuery(channel, eventType, language))
                .stream()
                .map(mapper::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(data, RequestIdUtil.resolve(request)));
    }

    @PutMapping("/{code}")
    @PreAuthorize("hasAuthority('SYSTEM_CONFIG')")
    public ResponseEntity<ApiResponse<NotificationTemplateResponse>> update(
            @PathVariable String code,
            @Valid @RequestBody UpdateNotificationTemplateRequest updateRequest,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] PUT /api/v1/notification-templates/{} | userId={}",
                code,
                LogMaskingUtil.maskId(principal.getId()));
        NotificationTemplateUpdateResult result = updateNotificationTemplateUseCase.execute(
                mapper.toUpdateCommand(code, updateRequest, principal),
                idempotencyKey);
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        if (result.replayed()) {
            builder.header("Idempotency-Replayed", "true");
        }
        return builder.body(ApiResponse.success(mapper.toResponse(result.template()), RequestIdUtil.resolve(request)));
    }

    @PostMapping("/{code}/preview")
    @PreAuthorize("hasAuthority('SYSTEM_CONFIG')")
    public ResponseEntity<ApiResponse<NotificationTemplatePreviewResponse>> preview(
            @PathVariable String code,
            @RequestBody Map<String, Object> sampleData,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] POST /api/v1/notification-templates/{}/preview | userId={}",
                code,
                LogMaskingUtil.maskId(principal.getId()));
        NotificationTemplatePreviewResult result = previewNotificationTemplateUseCase.execute(
                mapper.toPreviewCommand(code, sampleData, principal),
                idempotencyKey);
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        if (result.replayed()) {
            builder.header("Idempotency-Replayed", "true");
        }
        return builder.body(ApiResponse.success(mapper.toResponse(result), RequestIdUtil.resolve(request)));
    }
}
