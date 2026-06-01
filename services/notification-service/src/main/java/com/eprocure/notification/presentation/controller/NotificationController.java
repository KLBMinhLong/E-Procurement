package com.eprocure.notification.presentation.controller;

import com.eprocure.notification.application.service.MarkAllReadResult;
import com.eprocure.notification.application.service.NotificationReadResult;
import com.eprocure.notification.application.service.PageResult;
import com.eprocure.notification.application.usecase.CountUnreadNotificationsUseCase;
import com.eprocure.notification.application.usecase.ListMyNotificationsUseCase;
import com.eprocure.notification.application.usecase.MarkAllNotificationsReadUseCase;
import com.eprocure.notification.application.usecase.MarkNotificationReadUseCase;
import com.eprocure.notification.common.api.ApiResponse;
import com.eprocure.notification.common.api.RequestIdUtil;
import com.eprocure.notification.common.security.UserPrincipal;
import com.eprocure.notification.common.util.LogMaskingUtil;
import com.eprocure.notification.presentation.mapper.NotificationPresentationMapper;
import com.eprocure.notification.presentation.response.MarkAllReadResponse;
import com.eprocure.notification.presentation.response.NotificationResponse;
import com.eprocure.notification.presentation.response.UnreadCountResponse;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private static final Logger log = LogManager.getLogger(NotificationController.class);

    private final ListMyNotificationsUseCase listMyNotificationsUseCase;
    private final CountUnreadNotificationsUseCase countUnreadNotificationsUseCase;
    private final MarkNotificationReadUseCase markNotificationReadUseCase;
    private final MarkAllNotificationsReadUseCase markAllNotificationsReadUseCase;
    private final NotificationPresentationMapper mapper;

    public NotificationController(
            ListMyNotificationsUseCase listMyNotificationsUseCase,
            CountUnreadNotificationsUseCase countUnreadNotificationsUseCase,
            MarkNotificationReadUseCase markNotificationReadUseCase,
            MarkAllNotificationsReadUseCase markAllNotificationsReadUseCase,
            NotificationPresentationMapper mapper) {
        this.listMyNotificationsUseCase = listMyNotificationsUseCase;
        this.countUnreadNotificationsUseCase = countUnreadNotificationsUseCase;
        this.markNotificationReadUseCase = markNotificationReadUseCase;
        this.markAllNotificationsReadUseCase = markAllNotificationsReadUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW_OWN')")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> list(
            @RequestParam(value = "is_read", required = false) Boolean read,
            @RequestParam(value = "event_type", required = false) String eventType,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "30") int size,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/notifications | userId={}", LogMaskingUtil.maskId(principal.getId()));
        PageResult<com.eprocure.notification.application.service.NotificationView> result =
                listMyNotificationsUseCase.execute(mapper.toListQuery(principal, read, eventType, page, size));
        List<NotificationResponse> data = result.items().stream()
                .map(mapper::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.successWithMeta(data, result.meta(), RequestIdUtil.resolve(request)));
    }

    @GetMapping("/count")
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW_OWN')")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> countUnread(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/notifications/count | userId={}",
                LogMaskingUtil.maskId(principal.getId()));
        return ResponseEntity.ok(ApiResponse.success(
                new UnreadCountResponse(countUnreadNotificationsUseCase.execute(principal.getId())),
                RequestIdUtil.resolve(request)));
    }

    @PatchMapping("/{notificationId}/read")
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW_OWN')")
    public ResponseEntity<ApiResponse<NotificationResponse>> markRead(
            @PathVariable UUID notificationId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] PATCH /api/v1/notifications/{}/read | userId={}",
                LogMaskingUtil.maskId(notificationId),
                LogMaskingUtil.maskId(principal.getId()));
        NotificationReadResult result = markNotificationReadUseCase.execute(notificationId, principal.getId(), idempotencyKey);
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        if (result.replayed()) {
            builder.header("Idempotency-Replayed", "true");
        }
        return builder.body(ApiResponse.success(mapper.toResponse(result.notification()), RequestIdUtil.resolve(request)));
    }

    @PatchMapping("/read-all")
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW_OWN')")
    public ResponseEntity<ApiResponse<MarkAllReadResponse>> markAllRead(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] PATCH /api/v1/notifications/read-all | userId={}",
                LogMaskingUtil.maskId(principal.getId()));
        MarkAllReadResult result = markAllNotificationsReadUseCase.execute(principal.getId(), idempotencyKey);
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        if (result.replayed()) {
            builder.header("Idempotency-Replayed", "true");
        }
        return builder.body(ApiResponse.success(
                new MarkAllReadResponse(result.markedCount()),
                RequestIdUtil.resolve(request)));
    }
}
