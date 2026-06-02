package com.eprocure.vendor.presentation.controller;

import com.eprocure.vendor.application.service.PageResult;
import com.eprocure.vendor.application.service.RfqMutationResult;
import com.eprocure.vendor.application.usecase.CloseRfqUseCase;
import com.eprocure.vendor.application.usecase.CreateRfqUseCase;
import com.eprocure.vendor.application.usecase.GetRfqDetailUseCase;
import com.eprocure.vendor.application.usecase.ListRfqsUseCase;
import com.eprocure.vendor.common.api.ApiResponse;
import com.eprocure.vendor.common.api.RequestIdUtil;
import com.eprocure.vendor.common.security.UserPrincipal;
import com.eprocure.vendor.common.util.LogMaskingUtil;
import com.eprocure.vendor.domain.model.RfqStatus;
import com.eprocure.vendor.presentation.mapper.RfqPresentationMapper;
import com.eprocure.vendor.presentation.request.CreateRfqRequest;
import com.eprocure.vendor.presentation.response.RfqDetailResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rfq")
public class RfqController {
    private static final Logger log = LogManager.getLogger(RfqController.class);

    private final ListRfqsUseCase listRfqsUseCase;
    private final CreateRfqUseCase createRfqUseCase;
    private final GetRfqDetailUseCase getRfqDetailUseCase;
    private final CloseRfqUseCase closeRfqUseCase;
    private final RfqPresentationMapper mapper;

    public RfqController(
            ListRfqsUseCase listRfqsUseCase,
            CreateRfqUseCase createRfqUseCase,
            GetRfqDetailUseCase getRfqDetailUseCase,
            CloseRfqUseCase closeRfqUseCase,
            RfqPresentationMapper mapper) {
        this.listRfqsUseCase = listRfqsUseCase;
        this.createRfqUseCase = createRfqUseCase;
        this.getRfqDetailUseCase = getRfqDetailUseCase;
        this.closeRfqUseCase = closeRfqUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('RFQ_VIEW')")
    public ResponseEntity<ApiResponse<List<RfqDetailResponse>>> list(
            @RequestParam(value = "status", required = false) RfqStatus status,
            @RequestParam(value = "pr_id", required = false) UUID prId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", required = false) String sort,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/rfq | userId={}",
                LogMaskingUtil.maskId(principal.getId()));

        PageResult<com.eprocure.vendor.application.service.RfqDetailView> result = listRfqsUseCase.execute(
                mapper.toListQuery(principal, status, prId, page, size, sort));
        List<RfqDetailResponse> data = result.items().stream()
                .map(mapper::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.successWithMeta(data, result.meta(), RequestIdUtil.resolve(request)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('RFQ_CREATE')")
    public ResponseEntity<ApiResponse<RfqDetailResponse>> create(
            @Valid @RequestBody CreateRfqRequest body,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] POST /api/v1/rfq | userId={}",
                LogMaskingUtil.maskId(principal.getId()));

        RfqMutationResult result = createRfqUseCase.execute(mapper.toCreateCommand(principal, body), idempotencyKey);
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.CREATED);
        if (result.replayed()) {
            builder.header("Idempotency-Replayed", "true");
        }
        return builder.body(ApiResponse.success(mapper.toResponse(result.view()), RequestIdUtil.resolve(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('RFQ_VIEW')")
    public ResponseEntity<ApiResponse<RfqDetailResponse>> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/rfq/{} | userId={}",
                LogMaskingUtil.maskId(id),
                LogMaskingUtil.maskId(principal.getId()));

        RfqDetailResponse response = mapper.toResponse(getRfqDetailUseCase.execute(id, principal.getId()));
        return ResponseEntity.ok(ApiResponse.success(response, RequestIdUtil.resolve(request)));
    }

    @PatchMapping("/{id}/close")
    @PreAuthorize("hasAuthority('RFQ_EVALUATE')")
    public ResponseEntity<ApiResponse<Void>> close(
            @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] PATCH /api/v1/rfq/{}/close | userId={}",
                LogMaskingUtil.maskId(id),
                LogMaskingUtil.maskId(principal.getId()));

        RfqMutationResult result = closeRfqUseCase.execute(mapper.toCloseCommand(principal, id), idempotencyKey);
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        if (result.replayed()) {
            builder.header("Idempotency-Replayed", "true");
        }
        return builder.body(ApiResponse.<Void>success(null, RequestIdUtil.resolve(request)));
    }
}
