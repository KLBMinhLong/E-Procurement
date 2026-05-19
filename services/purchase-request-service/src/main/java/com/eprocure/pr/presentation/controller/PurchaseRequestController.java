package com.eprocure.pr.presentation.controller;

import com.eprocure.pr.application.service.CreatePurchaseRequestResult;
import com.eprocure.pr.application.usecase.CreatePurchaseRequestUseCase;
import com.eprocure.pr.common.api.ApiResponse;
import com.eprocure.pr.common.api.RequestIdUtil;
import com.eprocure.pr.common.security.UserPrincipal;
import com.eprocure.pr.common.util.LogMaskingUtil;
import com.eprocure.pr.presentation.mapper.PurchaseRequestPresentationMapper;
import com.eprocure.pr.presentation.request.CreatePrRequest;
import com.eprocure.pr.presentation.response.CreatedPurchaseRequestResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/purchase-requests")
public class PurchaseRequestController {
    private static final Logger log = LogManager.getLogger(PurchaseRequestController.class);

    private final CreatePurchaseRequestUseCase createPurchaseRequestUseCase;
    private final PurchaseRequestPresentationMapper mapper;

    public PurchaseRequestController(
            CreatePurchaseRequestUseCase createPurchaseRequestUseCase,
            PurchaseRequestPresentationMapper mapper) {
        this.createPurchaseRequestUseCase = createPurchaseRequestUseCase;
        this.mapper = mapper;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PR_CREATE')")
    public ResponseEntity<ApiResponse<CreatedPurchaseRequestResponse>> create(
            @Valid @RequestBody CreatePrRequest body,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] POST /api/v1/purchase-requests | userId={}",
                LogMaskingUtil.maskId(principal.getId()));
        CreatePurchaseRequestResult result = createPurchaseRequestUseCase.execute(
                mapper.toCommand(body, principal),
                idempotencyKey);
        HttpStatus status = result.replayed() ? HttpStatus.OK : HttpStatus.CREATED;
        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.status(status);
        if (result.replayed()) {
            responseBuilder.header("Idempotency-Replayed", "true");
        }
        return responseBuilder.body(ApiResponse.success(mapper.toResponse(result.view()), RequestIdUtil.resolve(request)));
    }
}
