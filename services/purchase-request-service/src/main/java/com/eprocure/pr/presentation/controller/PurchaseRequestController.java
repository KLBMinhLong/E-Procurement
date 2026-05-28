package com.eprocure.pr.presentation.controller;

import com.eprocure.pr.application.service.CreatePurchaseRequestResult;
import com.eprocure.pr.application.service.SubmitPurchaseRequestResult;
import com.eprocure.pr.application.service.UpdatedPurchaseRequestView;
import com.eprocure.pr.application.usecase.CancelPurchaseRequestUseCase;
import com.eprocure.pr.application.usecase.CreatePurchaseRequestUseCase;
import com.eprocure.pr.application.usecase.GetPurchaseRequestUseCase;
import com.eprocure.pr.application.usecase.GetPurchaseRequestUseCase.PagedResult;
import com.eprocure.pr.application.usecase.SubmitPurchaseRequestUseCase;
import com.eprocure.pr.application.usecase.UpdatePurchaseRequestUseCase;
import com.eprocure.pr.common.api.ApiResponse;
import com.eprocure.pr.common.api.RequestIdUtil;
import com.eprocure.pr.common.security.UserPrincipal;
import com.eprocure.pr.common.util.LogMaskingUtil;
import com.eprocure.pr.presentation.mapper.PurchaseRequestPresentationMapper;
import com.eprocure.pr.presentation.request.CancelPrRequest;
import com.eprocure.pr.presentation.request.CreatePrRequest;
import com.eprocure.pr.presentation.request.ListPrRequest;
import com.eprocure.pr.presentation.request.UpdatePrRequest;
import com.eprocure.pr.presentation.response.CreatedPurchaseRequestResponse;
import com.eprocure.pr.presentation.response.PurchaseRequestDetailResponse;
import com.eprocure.pr.presentation.response.PurchaseRequestSummaryResponse;
import com.eprocure.pr.presentation.response.SubmittedPurchaseRequestResponse;
import com.eprocure.pr.presentation.response.UpdatedPurchaseRequestResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/purchase-requests")
public class PurchaseRequestController {
    private static final Logger log = LogManager.getLogger(PurchaseRequestController.class);

    private final CreatePurchaseRequestUseCase createPurchaseRequestUseCase;
    private final SubmitPurchaseRequestUseCase submitPurchaseRequestUseCase;
    private final UpdatePurchaseRequestUseCase updatePurchaseRequestUseCase;
    private final CancelPurchaseRequestUseCase cancelPurchaseRequestUseCase;
    private final GetPurchaseRequestUseCase getPurchaseRequestUseCase;
    private final PurchaseRequestPresentationMapper mapper;

    public PurchaseRequestController(
            CreatePurchaseRequestUseCase createPurchaseRequestUseCase,
            SubmitPurchaseRequestUseCase submitPurchaseRequestUseCase,
            UpdatePurchaseRequestUseCase updatePurchaseRequestUseCase,
            CancelPurchaseRequestUseCase cancelPurchaseRequestUseCase,
            GetPurchaseRequestUseCase getPurchaseRequestUseCase,
            PurchaseRequestPresentationMapper mapper) {
        this.createPurchaseRequestUseCase = createPurchaseRequestUseCase;
        this.submitPurchaseRequestUseCase = submitPurchaseRequestUseCase;
        this.updatePurchaseRequestUseCase = updatePurchaseRequestUseCase;
        this.cancelPurchaseRequestUseCase = cancelPurchaseRequestUseCase;
        this.getPurchaseRequestUseCase = getPurchaseRequestUseCase;
        this.mapper = mapper;
    }

    // ── GET /purchase-requests  (list) ───────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAnyAuthority('PR_VIEW_OWN','PR_VIEW_DEPARTMENT','PR_VIEW_ALL')")
    public ResponseEntity<ApiResponse<List<PurchaseRequestSummaryResponse>>> list(
            @ModelAttribute ListPrRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {

        log.info("[CONTROLLER] GET /api/v1/purchase-requests | userId={}",
                LogMaskingUtil.maskId(principal.getId()));

        // Determine view scope from principal's permissions
        String viewScope = resolveViewScope(principal);
        PagedResult<com.eprocure.pr.application.service.PurchaseRequestSummaryView> result =
                getPurchaseRequestUseCase.getList(mapper.toQuery(request, principal, viewScope));

        List<PurchaseRequestSummaryResponse> data = result.content().stream()
                .map(mapper::toSummaryResponse)
                .toList();

        Map<String, Object> meta = Map.of(
                "page", result.page(),
                "size", result.size(),
                "totalElements", result.totalElements(),
                "totalPages", result.totalPages(),
                "isFirst", result.isFirst(),
                "isLast", result.isLast()
        );

        return ResponseEntity.ok(ApiResponse.successWithMeta(data, meta, RequestIdUtil.resolve(httpRequest)));
    }

    // ── GET /purchase-requests/{id}  (detail) ───────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('PR_VIEW_OWN','PR_VIEW_DEPARTMENT','PR_VIEW_ALL')")
    public ResponseEntity<ApiResponse<PurchaseRequestDetailResponse>> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {

        log.info("[CONTROLLER] GET /api/v1/purchase-requests/{} | userId={}",
                id, LogMaskingUtil.maskId(principal.getId()));

        PurchaseRequestDetailResponse response = mapper.toDetailResponse(
                getPurchaseRequestUseCase.getDetail(id, principal.getId()));

        return ResponseEntity.ok(ApiResponse.success(response, RequestIdUtil.resolve(request)));
    }

    // ── POST /purchase-requests  (create) ────────────────────────────────────

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

    // ── PUT /purchase-requests/{id}  (update) ────────────────────────────────

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PR_EDIT_OWN_DRAFT')")
    public ResponseEntity<ApiResponse<UpdatedPurchaseRequestResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePrRequest body,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {

        log.info("[CONTROLLER] PUT /api/v1/purchase-requests/{} | userId={}",
                id, LogMaskingUtil.maskId(principal.getId()));

        UpdatedPurchaseRequestView view = updatePurchaseRequestUseCase.execute(
                mapper.toUpdateCommand(id, body, principal),
                idempotencyKey);

        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(view), RequestIdUtil.resolve(request)));
    }

    // ── PATCH /purchase-requests/{id}/submit ─────────────────────────────────

    @PatchMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('PR_CREATE')")
    public ResponseEntity<ApiResponse<SubmittedPurchaseRequestResponse>> submit(
            @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {

        log.info("[CONTROLLER] PATCH /api/v1/purchase-requests/{}/submit | userId={}",
                id, LogMaskingUtil.maskId(principal.getId()));
        SubmitPurchaseRequestResult result = submitPurchaseRequestUseCase.execute(
                mapper.toCommand(id, principal),
                idempotencyKey);
        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok();
        if (result.replayed()) {
            responseBuilder.header("Idempotency-Replayed", "true");
        }
        return responseBuilder.body(ApiResponse.success(mapper.toResponse(result.view()), RequestIdUtil.resolve(request)));
    }

    // ── PATCH /purchase-requests/{id}/cancel ─────────────────────────────────

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('PR_CANCEL_OWN')")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @PathVariable UUID id,
            @Valid @RequestBody CancelPrRequest body,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {

        log.info("[CONTROLLER] PATCH /api/v1/purchase-requests/{}/cancel | userId={}",
                id, LogMaskingUtil.maskId(principal.getId()));

        cancelPurchaseRequestUseCase.execute(
                mapper.toCancelCommand(id, body, principal),
                idempotencyKey);

        return ResponseEntity.ok(ApiResponse.ok("PR_CANCELLED",
                "Yêu cầu mua sắm đã được hủy thành công",
                RequestIdUtil.resolve(request)));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Resolves view scope from principal's granted authorities.
     * Priority: ALL > DEPARTMENT > OWN (default).
     */
    private String resolveViewScope(UserPrincipal principal) {
        if (principal.getAuthorities().stream()
                .anyMatch(a -> "PR_VIEW_ALL".equals(a.getAuthority()))) {
            return "ALL";
        }
        if (principal.getAuthorities().stream()
                .anyMatch(a -> "PR_VIEW_DEPARTMENT".equals(a.getAuthority()))) {
            return "DEPARTMENT";
        }
        return "OWN";
    }
}
