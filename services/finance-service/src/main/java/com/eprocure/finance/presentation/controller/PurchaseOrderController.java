package com.eprocure.finance.presentation.controller;

import com.eprocure.finance.application.service.PageResult;
import com.eprocure.finance.application.service.PurchaseOrderActionResult;
import com.eprocure.finance.application.usecase.CancelPurchaseOrderUseCase;
import com.eprocure.finance.application.usecase.GetPurchaseOrderUseCase;
import com.eprocure.finance.application.usecase.ListPurchaseOrdersUseCase;
import com.eprocure.finance.application.usecase.SendPurchaseOrderUseCase;
import com.eprocure.finance.application.usecase.UpdatePurchaseOrderDraftUseCase;
import com.eprocure.finance.common.api.ApiResponse;
import com.eprocure.finance.common.api.RequestIdUtil;
import com.eprocure.finance.common.security.UserPrincipal;
import com.eprocure.finance.common.util.LogMaskingUtil;
import com.eprocure.finance.domain.model.PurchaseOrderStatus;
import com.eprocure.finance.presentation.mapper.PurchaseOrderPresentationMapper;
import com.eprocure.finance.presentation.request.CancelPurchaseOrderRequest;
import com.eprocure.finance.presentation.request.SendPurchaseOrderRequest;
import com.eprocure.finance.presentation.request.UpdatePurchaseOrderDraftRequest;
import com.eprocure.finance.presentation.response.PurchaseOrderResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/v1/purchase-orders")
public class PurchaseOrderController {
    private static final Logger log = LogManager.getLogger(PurchaseOrderController.class);

    private final ListPurchaseOrdersUseCase listPurchaseOrdersUseCase;
    private final GetPurchaseOrderUseCase getPurchaseOrderUseCase;
    private final UpdatePurchaseOrderDraftUseCase updatePurchaseOrderDraftUseCase;
    private final SendPurchaseOrderUseCase sendPurchaseOrderUseCase;
    private final CancelPurchaseOrderUseCase cancelPurchaseOrderUseCase;
    private final PurchaseOrderPresentationMapper mapper;

    public PurchaseOrderController(
            ListPurchaseOrdersUseCase listPurchaseOrdersUseCase,
            GetPurchaseOrderUseCase getPurchaseOrderUseCase,
            UpdatePurchaseOrderDraftUseCase updatePurchaseOrderDraftUseCase,
            SendPurchaseOrderUseCase sendPurchaseOrderUseCase,
            CancelPurchaseOrderUseCase cancelPurchaseOrderUseCase,
            PurchaseOrderPresentationMapper mapper) {
        this.listPurchaseOrdersUseCase = listPurchaseOrdersUseCase;
        this.getPurchaseOrderUseCase = getPurchaseOrderUseCase;
        this.updatePurchaseOrderDraftUseCase = updatePurchaseOrderDraftUseCase;
        this.sendPurchaseOrderUseCase = sendPurchaseOrderUseCase;
        this.cancelPurchaseOrderUseCase = cancelPurchaseOrderUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PO_VIEW_OWN') or hasAuthority('PO_VIEW_ALL')")
    public ResponseEntity<ApiResponse<List<PurchaseOrderResponse>>> list(
            @RequestParam(value = "status", required = false) PurchaseOrderStatus status,
            @RequestParam(value = "vendor_id", required = false) UUID vendorId,
            @RequestParam(value = "from_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "to_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", required = false) String sort,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/purchase-orders | userId={}",
                LogMaskingUtil.maskId(principal.getId()));
        PageResult<com.eprocure.finance.application.service.PurchaseOrderView> result =
                listPurchaseOrdersUseCase.execute(mapper.toListQuery(
                        principal,
                        status,
                        vendorId,
                        fromDate,
                        toDate,
                        page,
                        size,
                        sort));
        List<PurchaseOrderResponse> data = result.items().stream()
                .map(mapper::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.successWithMeta(data, result.meta(), RequestIdUtil.resolve(request)));
    }

    @GetMapping("/{poId}")
    @PreAuthorize("hasAuthority('PO_VIEW_OWN') or hasAuthority('PO_VIEW_ALL')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> get(
            @PathVariable UUID poId,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/purchase-orders/{} | userId={}",
                LogMaskingUtil.maskId(poId),
                LogMaskingUtil.maskId(principal.getId()));
        PurchaseOrderResponse response = mapper.toResponse(
                getPurchaseOrderUseCase.execute(mapper.toGetQuery(principal, poId)));
        return ResponseEntity.ok(ApiResponse.success(response, RequestIdUtil.resolve(request)));
    }

    @PatchMapping("/{poId}")
    @PreAuthorize("hasAuthority('PO_EDIT')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> updateDraft(
            @PathVariable UUID poId,
            @Valid @RequestBody UpdatePurchaseOrderDraftRequest body,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] PATCH /api/v1/purchase-orders/{} | userId={}",
                LogMaskingUtil.maskId(poId),
                LogMaskingUtil.maskId(principal.getId()));
        PurchaseOrderActionResult result = updatePurchaseOrderDraftUseCase.execute(
                mapper.toUpdateCommand(principal, poId, body),
                idempotencyKey);
        return actionResponse(result, request);
    }

    @PostMapping("/{poId}/send")
    @PreAuthorize("hasAuthority('PO_SEND_TO_VENDOR')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> send(
            @PathVariable UUID poId,
            @Valid @RequestBody(required = false) SendPurchaseOrderRequest body,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] POST /api/v1/purchase-orders/{}/send | userId={}",
                LogMaskingUtil.maskId(poId),
                LogMaskingUtil.maskId(principal.getId()));
        PurchaseOrderActionResult result = sendPurchaseOrderUseCase.execute(
                mapper.toSendCommand(principal, poId, body),
                idempotencyKey);
        return actionResponse(result, request);
    }

    @PatchMapping("/{poId}/cancel")
    @PreAuthorize("hasAuthority('PO_CANCEL')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> cancel(
            @PathVariable UUID poId,
            @Valid @RequestBody CancelPurchaseOrderRequest body,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] PATCH /api/v1/purchase-orders/{}/cancel | userId={}",
                LogMaskingUtil.maskId(poId),
                LogMaskingUtil.maskId(principal.getId()));
        PurchaseOrderActionResult result = cancelPurchaseOrderUseCase.execute(
                mapper.toCancelCommand(principal, poId, body),
                idempotencyKey);
        return actionResponse(result, request);
    }

    private ResponseEntity<ApiResponse<PurchaseOrderResponse>> actionResponse(
            PurchaseOrderActionResult result,
            HttpServletRequest request) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        if (result.replayed()) {
            builder.header("Idempotency-Replayed", "true");
        }
        return builder.body(ApiResponse.success(mapper.toResponse(result.view()), RequestIdUtil.resolve(request)));
    }
}
