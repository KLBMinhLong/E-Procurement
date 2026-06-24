package com.eprocure.iam.presentation.internal;

import com.eprocure.iam.application.port.in.InvalidateSessionCommand;
import com.eprocure.iam.application.port.in.ListActiveSessionsQuery;
import com.eprocure.iam.application.service.ActiveSessionView;
import com.eprocure.iam.application.service.InternalApiKeyGuard;
import com.eprocure.iam.application.service.PageResult;
import com.eprocure.iam.application.usecase.InvalidateSessionUseCase;
import com.eprocure.iam.application.usecase.ListActiveSessionsUseCase;
import com.eprocure.iam.common.api.ApiResponse;
import com.eprocure.iam.common.api.RequestIdUtil;
import com.eprocure.iam.common.util.LogMaskingUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/sessions")
public class InternalSessionController {
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";
    private static final Logger log = LogManager.getLogger(InternalSessionController.class);

    private final InternalApiKeyGuard internalApiKeyGuard;
    private final ListActiveSessionsUseCase listActiveSessionsUseCase;
    private final InvalidateSessionUseCase invalidateSessionUseCase;

    public InternalSessionController(
            InternalApiKeyGuard internalApiKeyGuard,
            ListActiveSessionsUseCase listActiveSessionsUseCase,
            InvalidateSessionUseCase invalidateSessionUseCase) {
        this.internalApiKeyGuard = internalApiKeyGuard;
        this.listActiveSessionsUseCase = listActiveSessionsUseCase;
        this.invalidateSessionUseCase = invalidateSessionUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ActiveSessionView>>> listActiveSessions(
            @RequestHeader(INTERNAL_API_KEY_HEADER) String internalApiKey,
            @RequestParam(name = "user_id", required = false) UUID userId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            HttpServletRequest request) {
        internalApiKeyGuard.verify(internalApiKey);
        log.info("[CONTROLLER] GET /internal/sessions | userId=internal | filterUserId={} | page={} | size={}",
                LogMaskingUtil.maskId(userId),
                page,
                size);
        PageResult<ActiveSessionView> result = listActiveSessionsUseCase.execute(new ListActiveSessionsQuery(
                page == null ? 1 : page,
                size == null ? 50 : size,
                userId));
        return ResponseEntity.ok(ApiResponse.success(result.items(), result.meta(), RequestIdUtil.resolve(request)));
    }

    @PatchMapping("/{sessionId}/invalidate")
    public ResponseEntity<ApiResponse<Void>> invalidateSession(
            @RequestHeader(INTERNAL_API_KEY_HEADER) String internalApiKey,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable UUID sessionId,
            @Valid @RequestBody InvalidateSessionInternalRequest body,
            HttpServletRequest request) {
        internalApiKeyGuard.verify(internalApiKey);
        log.info("[CONTROLLER] PATCH /internal/sessions/{}/invalidate | userId=internal | actorId={}",
                LogMaskingUtil.maskId(sessionId),
                LogMaskingUtil.maskId(body.actorId()));
        invalidateSessionUseCase.execute(
                new InvalidateSessionCommand(sessionId, body.actorId(), body.reason()),
                idempotencyKey);
        return ResponseEntity.ok(ApiResponse.successMessage("Session invalidated", RequestIdUtil.resolve(request)));
    }
}
